package com.smartretail.coupon.dto.request;

import com.smartretail.coupon.entity.Coupon.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

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
