package com.smartretail.coupon.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.coupon.dto.request.CouponRequest;
import com.smartretail.coupon.dto.response.CouponResponse;
import com.smartretail.coupon.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService coupons;

    @GetMapping
    public ApiResponse<List<CouponResponse>> list() {
        return ApiResponse.ok(coupons.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(coupons.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> update(@PathVariable UUID id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(coupons.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        coupons.delete(id);
    }
}
