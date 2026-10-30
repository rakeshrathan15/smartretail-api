package com.smartretail.inventory;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.inventory.InventoryDirectory.LevelFilter;
import com.smartretail.inventory.InventoryDtos.AdjustmentRequest;
import com.smartretail.inventory.InventoryDtos.AdjustmentRow;
import com.smartretail.inventory.InventoryDtos.InventoryRow;
import com.smartretail.inventory.InventoryDtos.InventorySummary;
import com.smartretail.inventory.InventoryDtos.ReceiptDetail;
import com.smartretail.inventory.InventoryDtos.ReceiptRequest;
import com.smartretail.inventory.InventoryDtos.ReceiptRow;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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
@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryDirectory directory;
    private final StockReceiptService receipts;
    private final StockAdjustmentService adjustments;

    public AdminInventoryController(InventoryDirectory directory, StockReceiptService receipts,
            StockAdjustmentService adjustments) {
        this.directory = directory;
        this.receipts = receipts;
        this.adjustments = adjustments;
    }

    @GetMapping
    public PageResponse<InventoryRow> stock(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "ALL") LevelFilter level,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return directory.stock(q, level, page, size);
    }

    @GetMapping("/summary")
    public InventorySummary summary() {
        return directory.summary();
    }

    @GetMapping("/adjustments")
    public PageResponse<AdjustmentRow> adjustments(
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return directory.adjustments(productId, page, size);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> adjust(@Valid @RequestBody AdjustmentRequest request, @AuthenticationPrincipal Jwt jwt) {
        return Map.of("id", adjustments.adjust(request, CurrentUser.id(jwt)));
    }

    @GetMapping("/receipts")
    public PageResponse<ReceiptRow> receipts(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return directory.receipts(supplierId, from, to, page, size);
    }

    @GetMapping("/receipts/{id}")
    public ReceiptDetail receipt(@PathVariable UUID id) {
        return directory.receipt(id);
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptDetail receive(@Valid @RequestBody ReceiptRequest request, @AuthenticationPrincipal Jwt jwt) {
        return directory.receipt(receipts.receive(request, CurrentUser.id(jwt)));
    }
}
