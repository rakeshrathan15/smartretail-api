package com.smartretail.coupon;

import com.smartretail.coupon.CouponDtos.CouponRequest;
import com.smartretail.coupon.CouponDtos.CouponResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
public class AdminCouponController {

    private final CouponService coupons;

    public AdminCouponController(CouponService coupons) {
        this.coupons = coupons;
    }

    @GetMapping
    public List<CouponResponse> list() {
        return coupons.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CouponRequest request) {
        return coupons.create(request);
    }

    @PutMapping("/{id}")
    public CouponResponse update(@PathVariable UUID id, @Valid @RequestBody CouponRequest request) {
        return coupons.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        coupons.delete(id);
    }
}
