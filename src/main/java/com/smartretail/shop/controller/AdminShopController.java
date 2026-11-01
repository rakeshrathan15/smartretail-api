package com.smartretail.shop.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.response.PageResponse;
import com.smartretail.security.CurrentUser;
import com.smartretail.shop.dto.request.ActiveRequest;
import com.smartretail.shop.dto.request.CreditRequest;
import com.smartretail.shop.dto.request.PaymentRequest;
import com.smartretail.shop.dto.request.ShopRequest;
import com.smartretail.shop.dto.response.CustomerOption;
import com.smartretail.shop.dto.response.Ledger;
import com.smartretail.shop.dto.response.ShopRow;
import com.smartretail.shop.dto.response.ShopSummary;
import com.smartretail.shop.repository.ShopDirectory.Standing;
import com.smartretail.shop.repository.ShopDirectory;
import com.smartretail.shop.service.ShopLedgerService;
import com.smartretail.shop.service.ShopService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Shops, their credit limits and what they owe. Admin-only (SecurityConfig). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/shops")
public class AdminShopController {

    private final ShopService shops;
    private final ShopDirectory directory;
    private final ShopLedgerService ledger;

    @GetMapping
    public ApiResponse<PageResponse<ShopRow>> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ALL") Standing standing,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(directory.find(q, standing, page, size));
    }

    @GetMapping("/summary")
    public ApiResponse<ShopSummary> summary() {
        return ApiResponse.ok(directory.summary());
    }

    @GetMapping("/customers")
    public ApiResponse<List<CustomerOption>> searchCustomers(@RequestParam(defaultValue = "") String q) {
        return ApiResponse.ok(shops.searchCustomers(q));
    }

    @PutMapping("/credit")
    public ApiResponse<ShopRow> setCredit(@Valid @RequestBody CreditRequest request) {
        return ApiResponse.ok(shops.setCredit(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopRow> create(@Valid @RequestBody ShopRequest request) {
        return ApiResponse.ok(shops.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShopRow> update(@PathVariable UUID id, @Valid @RequestBody ShopRequest request) {
        return ApiResponse.ok(shops.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<ShopRow> setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return ApiResponse.ok(shops.setActive(id, request.active()));
    }

    /** The shop with its balance, open credit orders and repayments. */
    @GetMapping("/{id}/ledger")
    public ApiResponse<Ledger> ledger(@PathVariable UUID id) {
        return ApiResponse.ok(ledger.ledger(id));
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Ledger> recordPayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(ledger.recordPayment(id, request, CurrentUser.id(jwt)));
    }

    @PostMapping("/{id}/payments/{paymentId}/void")
    public ApiResponse<Ledger> voidPayment(@PathVariable UUID id, @PathVariable UUID paymentId, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(ledger.voidPayment(id, paymentId, CurrentUser.id(jwt)));
    }
}
