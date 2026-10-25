package com.smartretail.order;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.catalog.Product;
import com.smartretail.catalog.ProductRepository;
import com.smartretail.common.ApiException;
import com.smartretail.coupon.CouponRepository;
import com.smartretail.order.AdminOrderDtos.AdminOrderDetail;
import com.smartretail.order.AdminOrderDtos.AdminOrderRow;
import com.smartretail.order.AdminOrderDtos.OrderStats;
import com.smartretail.order.AdminOrderDtos.PaymentDecision;
import com.smartretail.order.OrderEnums.PaymentMethod;
import com.smartretail.order.OrderEnums.PaymentStatus;
import com.smartretail.order.OrderEnums.Status;
import com.smartretail.storage.StorageService;
import jakarta.persistence.criteria.Predicate;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order handling for admins: find orders, move them through their statuses, check UPI payments. */
@Service
@Transactional
public class AdminOrderService {

    /** The business runs on India time; "today" and date filters use it. */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private static final List<Status> CLOSED = List.of(Status.CANCELLED, Status.RETURNED);

    private final OrderRepository orders;
    private final ProductRepository products;
    private final CouponRepository coupons;
    private final OrderMapper mapper;
    private final StorageService storage;
    private final Clock clock = Clock.systemUTC();

    public AdminOrderService(OrderRepository orders, ProductRepository products, CouponRepository coupons,
            OrderMapper mapper, StorageService storage) {
        this.orders = orders;
        this.products = products;
        this.coupons = coupons;
        this.mapper = mapper;
        this.storage = storage;
    }

    /** Filters for the orders list; every one is optional. Dates are inclusive days in India time. */
    public record Filter(Status status, PaymentMethod paymentMethod, PaymentStatus paymentStatus, String query,
            LocalDate from, LocalDate to) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderRow> list(Filter filter, int page, int size) {
        if (page < 0) {
            throw ApiException.badRequest("page must be 0 or more");
        }
        Page<Order> result = orders.findAll(toSpecification(filter),
                PageRequest.of(page, Math.clamp(size, 1, 100), Sort.by(Sort.Order.desc("createdAt"))));
        return new PageResponse<>(result.getContent().stream().map(AdminOrderRow::from).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderStats stats() {
        Instant startOfToday = LocalDate.now(clock.withZone(BUSINESS_ZONE)).atStartOfDay(BUSINESS_ZONE).toInstant();
        return new OrderStats(
                orders.countByCreatedAtGreaterThanEqual(startOfToday),
                orders.revenueSince(startOfToday, CLOSED),
                orders.countByStatus(Status.PLACED),
                orders.countByPaymentStatusAndStatusNotIn(PaymentStatus.PENDING_VERIFICATION, CLOSED),
                orders.sumUnpaid(PaymentMethod.COD, PaymentStatus.PENDING, CLOSED));
    }

    @Transactional(readOnly = true)
    public AdminOrderDetail get(UUID id) {
        return toDetail(orders.findWithItems(id).orElseThrow(() -> ApiException.notFound("Order not found")));
    }

    /**
     * Moves an order to {@code target}. Cancelled and returned orders give their cases back to stock;
     * a cancelled order also gives back its coupon use. Delivering a cash-on-delivery order marks it paid.
     */
    public AdminOrderDetail changeStatus(UUID id, Status target) {
        Order order = orders.lockById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        Status current = order.getStatus();
        if (!OrderWorkflow.canMove(current, target)) {
            throw conflict("An order that is " + label(current) + " cannot be marked " + label(target));
        }
        if (target == Status.CONFIRMED && order.getPaymentStatus() == PaymentStatus.PENDING_VERIFICATION) {
            throw conflict("Check the UPI payment screenshot before confirming this order");
        }
        if (target == Status.CONFIRMED && order.getPaymentStatus() == PaymentStatus.FAILED) {
            throw conflict("The UPI payment was rejected. Cancel the order or ask the shop to pay again");
        }

        if (OrderWorkflow.returnsStock(target)) {
            returnStock(order);
        }
        if (target == Status.CANCELLED && order.getCouponCode() != null) {
            coupons.release(order.getCouponCode());
        }
        if (target == Status.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD
                && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        order.setStatus(target);
        orders.flush();
        return get(id);
    }

    /** Approves (paid) or rejects (failed) a UPI payment after looking at its screenshot. */
    public AdminOrderDetail reviewPayment(UUID id, PaymentDecision decision) {
        Order order = orders.lockById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (order.getPaymentStatus() != PaymentStatus.PENDING_VERIFICATION) {
            throw conflict("This order has no payment waiting to be checked");
        }
        order.setPaymentStatus(decision == PaymentDecision.APPROVE ? PaymentStatus.PAID : PaymentStatus.FAILED);
        orders.flush();
        return get(id);
    }

    @Transactional(readOnly = true)
    public Optional<Path> paymentScreenshot(UUID id) {
        Order order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        return storage.privateFile(order.getPaymentScreenshot());
    }

    private void returnStock(Order order) {
        Map<UUID, Integer> quantities = order.getItems().stream()
                .filter(item -> item.getProductId() != null)
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity, Integer::sum));
        if (quantities.isEmpty()) {
            return;
        }
        Map<UUID, Product> locked = products.lockAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        // Products deleted since the order was placed have nothing to return to.
        locked.forEach((productId, product) ->
                product.setStockQuantity(product.getStockQuantity() + quantities.get(productId)));
    }

    private AdminOrderDetail toDetail(Order o) {
        boolean hasScreenshot = storage.privateFile(o.getPaymentScreenshot()).isPresent();
        return new AdminOrderDetail(mapper.toDetail(o), o.getUserId(), o.getShopId(), o.getShopName(),
                o.getCustomerName(), o.getCustomerPhone(), hasScreenshot, OrderWorkflow.nextStatuses(o.getStatus()),
                o.getPaymentStatus() == PaymentStatus.PENDING_VERIFICATION);
    }

    private static Specification<Order> toSpecification(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.paymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), f.paymentMethod()));
            }
            if (f.paymentStatus() != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), f.paymentStatus()));
            }
            if (f.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from().atStartOfDay(BUSINESS_ZONE).toInstant()));
            }
            if (f.to() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), f.to().plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()));
            }
            if (f.query() != null && !f.query().isBlank()) {
                String pattern = "%" + f.query().trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern, '\\'),
                        cb.like(cb.lower(root.get("shopName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("customerName")), pattern, '\\'),
                        cb.like(root.get("customerPhone"), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String label(Status status) {
        return status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
