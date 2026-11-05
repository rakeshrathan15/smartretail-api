package com.smartretail.inventory.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.response.PageResponse;
import com.smartretail.inventory.dto.request.AdjustmentRequest;
import com.smartretail.inventory.dto.request.ReceiptRequest;
import com.smartretail.inventory.dto.response.AdjustmentRow;
import com.smartretail.inventory.dto.response.InventoryRow;
import com.smartretail.inventory.dto.response.InventorySummary;
import com.smartretail.inventory.dto.response.ReceiptDetail;
import com.smartretail.inventory.dto.response.ReceiptRow;
import com.smartretail.inventory.repository.InventoryDirectory.LevelFilter;
import com.smartretail.inventory.repository.InventoryDirectory;
import com.smartretail.inventory.service.StockAdjustmentService;
import com.smartretail.inventory.service.StockReceiptService;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Stock levels, stock arriving and manual corrections. Admin-only (SecurityConfig). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryDirectory directory;
    private final StockReceiptService receipts;
    private final StockAdjustmentService adjustments;

    @GetMapping
    public ApiResponse<PageResponse<InventoryRow>> stock(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ALL") LevelFilter level,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(directory.stock(q, level, page, size));
    }

    @GetMapping("/summary")
    public ApiResponse<InventorySummary> summary() {
        return ApiResponse.ok(directory.summary());
    }

    @GetMapping("/adjustments")
    public ApiResponse<PageResponse<AdjustmentRow>> adjustments(
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(directory.adjustments(productId, page, size));
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, UUID>> adjust(@Valid @RequestBody AdjustmentRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(Map.of("id", adjustments.adjust(request, CurrentUser.id(jwt))));
    }

    @GetMapping("/receipts")
    public ApiResponse<PageResponse<ReceiptRow>> receipts(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(directory.receipts(supplierId, from, to, page, size));
    }

    @GetMapping("/receipts/{id}")
    public ApiResponse<ReceiptDetail> receipt(@PathVariable UUID id) {
        return ApiResponse.ok(directory.receipt(id));
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReceiptDetail> receive(@Valid @RequestBody ReceiptRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(directory.receipt(receipts.receive(request, CurrentUser.id(jwt))));
    }
}
