package com.smartretail.coupon;

import com.smartretail.common.ApiException;
import com.smartretail.coupon.Coupon.DiscountType;
import com.smartretail.coupon.CouponDtos.CouponRequest;
import com.smartretail.coupon.CouponDtos.CouponResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin management of coupons. Redeeming one at checkout is in the order package. */
@Service
@Transactional
public class CouponService {

    private final CouponRepository coupons;

    public CouponService(CouponRepository coupons) {
        this.coupons = coupons;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> list() {
        return coupons.findAllByOrderByCreatedAtDesc().stream().map(CouponResponse::from).toList();
    }

    public CouponResponse create(CouponRequest request) {
        String code = request.code().toUpperCase(Locale.ROOT);
        if (coupons.existsByCode(code)) {
            throw ApiException.conflict("Coupon " + code + " already exists");
        }
        Coupon coupon = new Coupon(code);
        apply(coupon, request);
        return CouponResponse.from(coupons.save(coupon));
    }

    public CouponResponse update(UUID id, CouponRequest request) {
        Coupon coupon = find(id);
        String code = request.code().toUpperCase(Locale.ROOT);
        if (!code.equals(coupon.getCode()) && coupons.existsByCode(code)) {
            throw ApiException.conflict("Coupon " + code + " already exists");
        }
        coupon.setCode(code);
        apply(coupon, request);
        return CouponResponse.from(coupon);
    }

    /** Coupons that were used stay for the order history; deactivate those instead. */
    public void delete(UUID id) {
        Coupon coupon = find(id);
        if (coupon.getUsedCount() > 0) {
            throw ApiException.conflict("This coupon has been used " + coupon.getUsedCount()
                    + " times; deactivate it instead of deleting");
        }
        coupons.delete(coupon);
    }

    private static void apply(Coupon coupon, CouponRequest r) {
        if (r.discountType() == DiscountType.PERCENTAGE && r.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest("A percentage discount cannot be above 100");
        }
        if (r.validFrom() != null && r.validUntil() != null && r.validUntil().isBefore(r.validFrom())) {
            throw ApiException.badRequest("The end date is before the start date");
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
        return coupons.findById(id).orElseThrow(() -> ApiException.notFound("Coupon not found"));
    }
}
