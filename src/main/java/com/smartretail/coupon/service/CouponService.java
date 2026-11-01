package com.smartretail.coupon.service;

import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.coupon.dto.request.CouponRequest;
import com.smartretail.coupon.dto.response.CouponResponse;
import com.smartretail.coupon.entity.Coupon.DiscountType;
import com.smartretail.coupon.entity.Coupon;
import com.smartretail.coupon.mapper.CouponMapper;
import com.smartretail.coupon.repository.CouponRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin management of coupons. Redeeming one at checkout is in the order package. */
@RequiredArgsConstructor
@Service
@Transactional
public class CouponService {

    private final CouponMapper couponMapper;
    private final CouponRepository coupons;

    @Transactional(readOnly = true)
    public List<CouponResponse> list() {
        return coupons.findAllByOrderByCreatedAtDesc().stream().map(couponMapper::toResponse).toList();
    }

    public CouponResponse create(CouponRequest request) {
        String code = request.code().toUpperCase(Locale.ROOT);
        if (coupons.existsByCode(code)) {
            throw new ApiException(ErrorCode.COUPON_ALREADY_EXISTS, "Coupon " + code + " already exists");
        }
        Coupon coupon = new Coupon(code);
        apply(coupon, request);
        return couponMapper.toResponse(coupons.save(coupon));
    }

    public CouponResponse update(UUID id, CouponRequest request) {
        Coupon coupon = find(id);
        String code = request.code().toUpperCase(Locale.ROOT);
        if (!code.equals(coupon.getCode()) && coupons.existsByCode(code)) {
            throw new ApiException(ErrorCode.COUPON_ALREADY_EXISTS, "Coupon " + code + " already exists");
        }
        coupon.setCode(code);
        apply(coupon, request);
        return couponMapper.toResponse(coupon);
    }

    /** Coupons that were used stay for the order history; deactivate those instead. */
    public void delete(UUID id) {
        Coupon coupon = find(id);
        if (coupon.getUsedCount() > 0) {
            throw new ApiException(ErrorCode.COUPON_IN_USE, "This coupon has been used " + coupon.getUsedCount()
                    + " times; deactivate it instead of deleting");
        }
        coupons.delete(coupon);
    }

    private static void apply(Coupon coupon, CouponRequest r) {
        if (r.discountType() == DiscountType.PERCENTAGE && r.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ApiException(ErrorCode.INVALID_COUPON_VALUE, "A percentage discount cannot be above 100");
        }
        if (r.validFrom() != null && r.validUntil() != null && r.validUntil().isBefore(r.validFrom())) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE, "The end date is before the start date");
        }
        coupon.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        coupon.setDiscountType(r.discountType());
        coupon.setDiscountValue(r.discountValue());
        coupon.setMinOrderValue(r.minOrderValue() == null ? BigDecimal.ZERO : r.minOrderValue());
        coupon.setMaxDiscount(r.maxDiscount());
        coupon.setUsageLimit(r.usageLimit());
        coupon.setValidFrom(r.validFrom());
        coupon.setValidUntil(r.validUntil());
        if (r.active() != null) {
            coupon.setActive(r.active());
        }
    }

    private Coupon find(UUID id) {
        return coupons.findById(id).orElseThrow(() -> new ApiException(ErrorCode.COUPON_NOT_FOUND, "Coupon not found"));
    }
}
