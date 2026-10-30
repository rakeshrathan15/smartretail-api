package com.smartretail.coupon;

import com.smartretail.coupon.Coupon.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class CouponDtos {

    private CouponDtos() {
    }

    public record CouponRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,20}", message = "use 3-20 letters, digits, - or _") String code,
            @Size(max = 200) String description,
            @NotNull DiscountType discountType,
            @NotNull @Positive BigDecimal discountValue,
            @DecimalMin("0.00") BigDecimal minOrderValue,
            @Positive BigDecimal maxDiscount,
            @Positive Integer usageLimit,
            Instant validFrom,
            Instant validUntil,
            Boolean active) {
    }

    public record CouponResponse(
            UUID id, String code, String description, DiscountType discountType, BigDecimal discountValue,
            BigDecimal minOrderValue, BigDecimal maxDiscount, Integer usageLimit, int usedCount, Instant validFrom,
            Instant validUntil, boolean active) {

        static CouponResponse from(Coupon c) {
            return new CouponResponse(c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(),
                    c.getDiscountValue(), c.getMinOrderValue(), c.getMaxDiscount(), c.getUsageLimit(), c.getUsedCount(),
                    c.getValidFrom(), c.getValidUntil(), c.isActive());
        }
    }
}
