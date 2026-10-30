package com.smartretail.order;

import com.smartretail.address.Address;
import com.smartretail.address.AddressRepository;
import com.smartretail.catalog.Product;
import com.smartretail.catalog.ProductImage;
import com.smartretail.catalog.ProductImageRepository;
import com.smartretail.catalog.ProductRepository;
import com.smartretail.common.ApiException;
import com.smartretail.common.Money;
import com.smartretail.coupon.Coupon;
import com.smartretail.coupon.CouponRepository;
import com.smartretail.order.OrderDtos.AddressSnapshot;
import com.smartretail.order.OrderDtos.CartItem;
import com.smartretail.order.OrderDtos.CreditInfo;
import com.smartretail.order.OrderDtos.OrderDetail;
import com.smartretail.route.DeliverySchedule;
import com.smartretail.order.OrderDtos.PlaceOrderRequest;
import com.smartretail.order.OrderDtos.Quote;
import com.smartretail.order.OrderEnums.Channel;
import com.smartretail.order.OrderEnums.PaymentMethod;
import com.smartretail.order.OrderEnums.PaymentStatus;
import com.smartretail.order.OrderEnums.PaymentTerms;
import com.smartretail.settings.StoreSettings;
import com.smartretail.shop.Shop;
import com.smartretail.shop.ShopRepository;
import com.smartretail.storage.StorageService;
import com.smartretail.user.Profile;
import com.smartretail.user.ProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CheckoutService {

    static final String PAYMENT_FOLDER = "payments";

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

    public CheckoutService(ProductRepository products, ProductImageRepository images, CouponRepository coupons,
            ShopRepository shops, AddressRepository addresses, ProfileRepository profiles, OrderRepository orders,
            StoreSettings settings, OrderMapper mapper, JsonMapper json, StorageService storage,
            DeliverySchedule schedule) {
        this.products = products;
        this.images = images;
        this.coupons = coupons;
        this.shops = shops;
        this.addresses = addresses;
        this.profiles = profiles;
        this.orders = orders;
        this.settings = settings;
        this.mapper = mapper;
        this.json = json;
        this.storage = storage;
        this.schedule = schedule;
    }

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
                .orElseThrow(() -> ApiException.notFound("Choose a delivery address"));
        PaymentMethod method = request.paymentMethod();
        if (method == PaymentMethod.CASH) {
            throw ApiException.badRequest("Cash is only for counter sales; choose cash on delivery");
        }

        Map<UUID, Integer> merged = CheckoutPricing.mergeItems(request.items());
        Map<UUID, Product> locked = byId(products.lockAllById(merged.keySet()));
        String couponCode = CheckoutPricing.normalizeCoupon(request.couponCode());
        Quote quote = priceCart(userId, merged, locked, couponCode);

        if (!quote.problems().isEmpty()) {
            throw conflict(quote.problems().getFirst());
        }
        if (quote.couponProblem() != null) {
            throw conflict(quote.couponProblem());
        }
        if (request.expectedTotal() != null && request.expectedTotal().compareTo(quote.total()) != 0) {
            throw conflict("Prices have changed. Please review your cart: the total is now " + Money.format(quote.total()));
        }

        Shop shop = shops.findFirstByOwnerUserIdOrderByCreatedAtAsc(userId).orElse(null);
        PaymentStatus paymentStatus = PaymentStatus.PENDING;
        PaymentTerms terms = PaymentTerms.PREPAID;
        String screenshot = null;
        switch (method) {
            case CREDIT -> {
                if (shop == null || shop.getCreditLimit().signum() <= 0) {
                    throw conflict("Credit is not enabled for your shop. Choose another payment method.");
                }
                shops.lockById(shop.getId());
                CreditInfo credit = creditOf(shop);
                if (quote.total().compareTo(credit.available()) > 0) {
                    throw conflict("Not enough credit: " + Money.format(credit.available().max(BigDecimal.ZERO))
                            + " available. Pay part of your dues or choose another payment method.");
                }
                terms = PaymentTerms.CREDIT;
            }
            case UPI -> {
                screenshot = request.paymentScreenshot();
                if (screenshot == null || !screenshot.startsWith(PAYMENT_FOLDER + "/")
                        || storage.privateFile(screenshot).isEmpty()) {
                    throw ApiException.badRequest("Upload the screenshot of your UPI payment");
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
            throw conflict("This coupon has just been fully used");
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

        return mapper.toDetail(orders.saveAndFlush(order));
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

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
