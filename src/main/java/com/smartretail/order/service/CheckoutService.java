package com.smartretail.order.service;

import com.smartretail.address.entity.Address;
import com.smartretail.address.repository.AddressRepository;
import com.smartretail.catalog.entity.Product;
import com.smartretail.catalog.entity.ProductImage;
import com.smartretail.catalog.repository.ProductImageRepository;
import com.smartretail.catalog.repository.ProductRepository;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.util.Money;
import com.smartretail.coupon.entity.Coupon;
import com.smartretail.coupon.repository.CouponRepository;
import com.smartretail.order.dto.request.CartItem;
import com.smartretail.order.dto.request.PlaceOrderRequest;
import com.smartretail.order.dto.response.AddressSnapshot;
import com.smartretail.order.dto.response.CreditInfo;
import com.smartretail.order.dto.response.OrderDetail;
import com.smartretail.order.dto.response.Quote;
import com.smartretail.order.entity.Order;
import com.smartretail.order.enums.OrderEnums.Channel;
import com.smartretail.order.enums.OrderEnums.PaymentMethod;
import com.smartretail.order.enums.OrderEnums.PaymentStatus;
import com.smartretail.order.enums.OrderEnums.PaymentTerms;
import com.smartretail.order.mapper.OrderMapper;
import com.smartretail.order.repository.OrderRepository;
import com.smartretail.order.support.CheckoutPricing;
import com.smartretail.route.service.DeliverySchedule;
import com.smartretail.settings.service.StoreSettings;
import com.smartretail.shop.entity.Shop;
import com.smartretail.shop.repository.ShopRepository;
import com.smartretail.storage.service.StorageService;
import com.smartretail.user.entity.Profile;
import com.smartretail.user.repository.ProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
@Slf4j
@Service
public class CheckoutService {

    public static final String PAYMENT_FOLDER = "payments";

    private final ProductRepository products;
    private final ProductImageRepository images;
    private final CouponRepository coupons;
    private final ShopRepository shops;
    private final AddressRepository addresses;
    private final ProfileRepository profiles;
    private final OrderRepository orders;
    private final StoreSettings settings;
    private final OrderMapper mapper;
    private final JsonMapper json;
    private final StorageService storage;
    private final DeliverySchedule schedule;
    private final Clock clock = Clock.systemUTC();

    /** Prices a cart without changing anything. {@code userId} is null for a visitor who has not signed in. */
    @Transactional(readOnly = true)
    public Quote quote(UUID userId, List<CartItem> items, String couponCode) {
        Map<UUID, Integer> merged = CheckoutPricing.mergeItems(items);
        Map<UUID, Product> found = byId(products.findAllById(merged.keySet()));
        return priceCart(userId, merged, found, CheckoutPricing.normalizeCoupon(couponCode));
    }

    /**
     * Places an order: re-prices everything, checks stock, coupon and credit under row locks,
     * takes the stock and saves the order. Any problem refuses the whole order.
     */
    @Transactional
    public OrderDetail placeOrder(UUID userId, PlaceOrderRequest request) {
        Address address = addresses.findByIdAndUserId(request.addressId(), userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADDRESS_NOT_FOUND, "Choose a delivery address"));
        PaymentMethod method = request.paymentMethod();
        if (method == PaymentMethod.CASH) {
            throw new ApiException(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED, "Cash is only for counter sales; choose cash on delivery");
        }

        Map<UUID, Integer> merged = CheckoutPricing.mergeItems(request.items());
        Map<UUID, Product> locked = byId(products.lockAllById(merged.keySet()));
        String couponCode = CheckoutPricing.normalizeCoupon(request.couponCode());
        Quote quote = priceCart(userId, merged, locked, couponCode);

        if (!quote.problems().isEmpty()) {
            throw new ApiException(ErrorCode.ORDER_CANNOT_BE_PLACED, quote.problems().getFirst());
        }
        if (quote.couponProblem() != null) {
            throw new ApiException(ErrorCode.COUPON_NOT_APPLICABLE, quote.couponProblem());
        }
        if (request.expectedTotal() != null && request.expectedTotal().compareTo(quote.total()) != 0) {
            throw new ApiException(ErrorCode.PRICE_CHANGED, "Prices have changed. Please review your cart: the total is now " + Money.format(quote.total()));
        }

        Shop shop = shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(userId).orElse(null);
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentTerms terms = PaymentTerms.PREPAID;
        String screenshot = null;
        switch (method) {
            case CREDIT -> {
                if (shop == null || !shop.isActive() || shop.getCreditLimit().signum() <= 0) {
                    throw new ApiException(ErrorCode.CREDIT_NOT_ENABLED, "Credit is not enabled for your shop. Choose another payment method.");
                }
                shops.lockById(shop.getId());
                CreditInfo credit = creditOf(shop);
                if (quote.total().compareTo(credit.available()) > 0) {
                    throw new ApiException(ErrorCode.INSUFFICIENT_CREDIT, "Not enough credit: " + Money.format(credit.available().max(BigDecimal.ZERO))
                            + " available. Pay part of your dues or choose another payment method.");
                }
                terms = PaymentTerms.CREDIT;
            }
            case UPI -> {
                screenshot = request.paymentScreenshot();
                if (screenshot == null || !screenshot.startsWith(PAYMENT_FOLDER + "/")
                        || storage.privateFile(screenshot).isEmpty()) {
                    throw new ApiException(ErrorCode.PAYMENT_SCREENSHOT_REQUIRED, "Upload the screenshot of your UPI payment");
                }
                paymentStatus = PaymentStatus.PENDING_VERIFICATION;
            }
            default -> {
                // Cash on delivery: paid when the order arrives.
            }
        }

        for (Map.Entry<UUID, Integer> item : merged.entrySet()) {
            Product product = locked.get(item.getKey());
            product.setStockQuantity(product.getStockQuantity() - item.getValue());
        }
        if (couponCode != null && coupons.redeem(coupons.findByCode(couponCode).orElseThrow().getId()) == 0) {
            throw new ApiException(ErrorCode.COUPON_LIMIT_REACHED, "This coupon has just been fully used");
        }

        Profile profile = profiles.findById(userId).orElse(null);
        Order order = new Order("SR" + orders.nextOrderNumber(), userId, Channel.ONLINE);
        order.setTotals(quote.subtotal(), quote.couponDiscount(), quote.deliveryCharge(), quote.total(),
                quote.totalCases());
        order.setPayment(method, paymentStatus, terms, screenshot);
        order.setCouponCode(couponCode);
        order.setCustomer(shop == null ? null : shop.getId(),
                shop != null ? shop.getName() : profile == null ? null : profile.getShopName(),
                address.getFullName(), address.getPhone(),
                profile == null ? null : profile.getDeliveryRouteId());
        order.setDeliveryDate(schedule.deliveryDateForOrderPlacedToday(order.getDeliveryRouteId()));
        order.setShippingAddress(json.writeValueAsString(new AddressSnapshot(address.getLabel(), address.getFullName(),
                address.getPhone(), address.getAddressLine1(), address.getAddressLine2(), address.getCity(),
                address.getState(), address.getPincode())));
        order.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        quote.lines().forEach(line -> order.addItem(line.productId(), line.name(), line.quantity(), line.pricePerCase(),
                locked.get(line.productId()).getCostPrice(), line.lineTotal()));

        Order saved = orders.saveAndFlush(order);
        log.info("Order placed: orderId={} orderNumber={} userId={} shopId={} method={} total={}", saved.getId(),
                saved.getOrderNumber(), userId, saved.getShopId(), method, saved.getTotal());
        return mapper.toDetail(saved);
    }

    private Quote priceCart(UUID userId, Map<UUID, Integer> merged, Map<UUID, Product> found, String couponCode) {
        Map<UUID, String> thumbnails = new HashMap<>();
        if (!found.isEmpty()) {
            for (ProductImage image : images.findForProducts(List.copyOf(found.keySet()))) {
                thumbnails.putIfAbsent(image.getProduct().getId(), image.getUrl());
            }
        }
        Coupon coupon = couponCode == null ? null : coupons.findByCode(couponCode).orElse(null);
        CreditInfo credit = userId == null ? null : shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(userId)
                .filter(s -> s.isActive() && s.getCreditLimit().signum() > 0)
                .map(this::creditOf)
                .orElse(null);
        return CheckoutPricing.quote(merged, found, thumbnails, couponCode, coupon, settings.checkout(), credit,
                clock.instant());
    }

    private CreditInfo creditOf(Shop shop) {
        BigDecimal outstanding = shops.outstanding(shop.getId());
        return new CreditInfo(shop.getCreditLimit(), outstanding, shop.getCreditLimit().subtract(outstanding));
    }

    private static Map<UUID, Product> byId(List<Product> list) {
        return list.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
