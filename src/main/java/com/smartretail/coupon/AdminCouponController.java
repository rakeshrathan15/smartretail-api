package com.smartretail.coupon;

import com.smartretail.common.ApiException;
import com.smartretail.coupon.Coupon.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (enforced in SecurityConfig for /api/admin/**). */
@RestController
@RequestMapping("/api/admin/coupons")
@Transactional
public class AdminCouponController {

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

    private final CouponRepository coupons;

    public AdminCouponController(CouponRepository coupons) {
        this.coupons = coupons;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CouponResponse> list() {
        return coupons.findAllByOrderByCreatedAtDesc().stream().map(CouponResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CouponRequest request) {
        String code = request.code().toUpperCase(Locale.ROOT);
        if (coupons.existsByCode(code)) {
            throw ApiException.conflict("Coupon " + code + " already exists");
        }
        Coupon coupon = new Coupon(code);
        apply(coupon, request);
        return CouponResponse.from(coupons.save(coupon));
    }

    @PutMapping("/{id}")
    public CouponResponse update(@PathVariable UUID id, @Valid @RequestBody CouponRequest request) {
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
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
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
