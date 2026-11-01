package com.smartretail.coupon.entity;

import com.smartretail.common.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

    public enum DiscountType {
        PERCENTAGE,
        FIXED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @Setter
    private String code;

    @Setter
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    @Setter
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    @Setter
    private BigDecimal discountValue;

    @Column(name = "min_order_value", nullable = false)
    @Setter
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount")
    @Setter
    private BigDecimal maxDiscount;

    @Column(name = "usage_limit")
    @Setter
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "valid_from")
    @Setter
    private Instant validFrom;

    @Column(name = "valid_until")
    @Setter
    private Instant validUntil;

    @Column(name = "is_active", nullable = false)
    @Setter
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter(AccessLevel.NONE)
    private Instant updatedAt;

    public Coupon(String code) {
        this.code = code;
    }

    /** Why this coupon cannot be used on an order worth {@code subtotal}, or empty when it can. */
    public Optional<String> problemFor(BigDecimal subtotal, Instant now) {
        if (!active || (validUntil != null && now.isAfter(validUntil))) {
            return Optional.of("This coupon has expired");
        }
        if (validFrom != null && now.isBefore(validFrom)) {
            return Optional.of("This coupon is not active yet");
        }
        if (usageLimit != null && usedCount >= usageLimit) {
            return Optional.of("This coupon has been fully used");
        }
        if (subtotal.compareTo(minOrderValue) < 0) {
            return Optional.of("Add items worth " + Money.format(minOrderValue) + " or more to use this coupon");
        }
        return Optional.empty();
    }

    /** The discount on {@code subtotal}: a percentage capped at {@code maxDiscount}, never more than the subtotal. */
    public BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal discount = discountType == DiscountType.PERCENTAGE
                ? subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : discountValue;
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
}
