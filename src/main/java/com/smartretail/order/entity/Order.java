package com.smartretail.order.entity;

import com.smartretail.order.enums.OrderEnums.Channel;
import com.smartretail.order.enums.OrderEnums.PaymentMethod;
import com.smartretail.order.enums.OrderEnums.PaymentStatus;
import com.smartretail.order.enums.OrderEnums.PaymentTerms;
import com.smartretail.order.enums.OrderEnums.Status;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** An order with a snapshot of prices, items and delivery address taken when it was placed. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "shop_id")
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Status status = Status.PLACED;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "shipping_charge", nullable = false)
    private BigDecimal shippingCharge;

    @Column(nullable = false)
    private BigDecimal discount;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "total_cases", nullable = false)
    private int totalCases;

    @Column(name = "coupon_code")
    @Setter
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Setter
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", nullable = false)
    private PaymentTerms paymentTerms;

    @Column(name = "payment_screenshot")
    private String paymentScreenshot;

    /** JSON of {@link AddressSnapshot}. Stored as jsonb; the cast lets a plain string be written. */
    @Column(name = "shipping_address", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    @Setter
    private String shippingAddress;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "delivery_route_id")
    private UUID deliveryRouteId;

    /** The day the order goes out on its route's van; pick-lists are made per route and date. */
    @Column(name = "delivery_date")
    @Setter
    private LocalDate deliveryDate;

    @Setter
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Order(String orderNumber, UUID userId, Channel channel) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.channel = channel;
    }

    public void addItem(UUID productId, String productName, int quantity, BigDecimal price, BigDecimal costPrice,
            BigDecimal total) {
        items.add(new OrderItem(this, productId, productName, quantity, price, costPrice, total));
    }

    public void setTotals(BigDecimal subtotal, BigDecimal discount, BigDecimal shippingCharge, BigDecimal total,
            int totalCases) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.shippingCharge = shippingCharge;
        this.total = total;
        this.totalCases = totalCases;
    }

    public void setPayment(PaymentMethod method, PaymentStatus status, PaymentTerms terms, String screenshot) {
        this.paymentMethod = method;
        this.paymentStatus = status;
        this.paymentTerms = terms;
        this.paymentScreenshot = screenshot;
    }

    public void setCustomer(UUID shopId, String shopName, String customerName, String customerPhone, UUID deliveryRouteId) {
        this.shopId = shopId;
        this.shopName = shopName;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryRouteId = deliveryRouteId;
    }
}
