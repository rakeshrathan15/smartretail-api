package com.smartretail.coupon.dto.response;

import com.smartretail.coupon.entity.Coupon.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id, String code, String description, DiscountType discountType, BigDecimal discountValue,
        BigDecimal minOrderValue, BigDecimal maxDiscount, Integer usageLimit, int usedCount, Instant validFrom,
        Instant validUntil, boolean active) {
}
