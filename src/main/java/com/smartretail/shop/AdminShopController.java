package com.smartretail.shop;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.security.CurrentUser;
import com.smartretail.shop.ShopDirectory.Standing;
import com.smartretail.shop.ShopDtos.ActiveRequest;
import com.smartretail.shop.ShopDtos.CreditRequest;
import com.smartretail.shop.ShopDtos.CustomerOption;
import com.smartretail.shop.ShopDtos.Ledger;
import com.smartretail.shop.ShopDtos.PaymentRequest;
import com.smartretail.shop.ShopDtos.ShopRequest;
import com.smartretail.shop.ShopDtos.ShopRow;
import com.smartretail.shop.ShopDtos.ShopSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
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
@RestController
@RequestMapping("/api/admin/shops")
public class AdminShopController {

    private final ShopService shops;
    private final ShopDirectory directory;
    private final ShopLedgerService ledger;

    public AdminShopController(ShopService shops, ShopDirectory directory, ShopLedgerService ledger) {
        this.shops = shops;
        this.directory = directory;
        this.ledger = ledger;
    }

    @GetMapping
    public PageResponse<ShopRow> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ALL") Standing standing,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return directory.find(q, standing, page, size);
    }

    @GetMapping("/summary")
    public ShopSummary summary() {
        return directory.summary();
    }

    @GetMapping("/customers")
    public List<CustomerOption> searchCustomers(@RequestParam(defaultValue = "") String q) {
        return shops.searchCustomers(q);
    }

    @PutMapping("/credit")
    public ShopRow setCredit(@Valid @RequestBody CreditRequest request) {
        return shops.setCredit(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShopRow create(@Valid @RequestBody ShopRequest request) {
        return shops.create(request);
    }

    @PutMapping("/{id}")
    public ShopRow update(@PathVariable UUID id, @Valid @RequestBody ShopRequest request) {
        return shops.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public ShopRow setActive(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return shops.setActive(id, request.active());
    }

    /** The shop with its balance, open credit orders and repayments. */
    @GetMapping("/{id}/ledger")
    public Ledger ledger(@PathVariable UUID id) {
        return ledger.ledger(id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Ledger recordPayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ledger.recordPayment(id, request, CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/payments/{paymentId}/void")
    public Ledger voidPayment(@PathVariable UUID id, @PathVariable UUID paymentId, @AuthenticationPrincipal Jwt jwt) {
        return ledger.voidPayment(id, paymentId, CurrentUser.id(jwt));
    }
}
