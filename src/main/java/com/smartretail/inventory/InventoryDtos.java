package com.smartretail.inventory;

import com.smartretail.inventory.StockAdjustment.Reason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record ReceiptItemRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) @Max(100000) Integer quantity,
            @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99")
            @Digits(integer = 8, fraction = 2, message = "must be in rupees and paise") BigDecimal costPrice) {
    }

    public record ReceiptRequest(
            UUID supplierId,
            UUID storeId,
            @NotNull LocalDate receiptDate,
            @Size(max = 60) String invoiceNumber,
            @Size(max = 500) String notes,
            @NotEmpty @Size(max = 200) List<@Valid ReceiptItemRequest> items) {
    }

    public record ReceiptRow(
            UUID id, LocalDate receiptDate, String supplierName, String storeName, String invoiceNumber,
            BigDecimal totalCost, int itemCount, int totalCases, String createdBy, Instant createdAt) {
    }

    public record ReceiptItemRow(UUID productId, String productName, int quantity, BigDecimal costPrice,
            BigDecimal total) {
    }

    public record ReceiptDetail(ReceiptRow receipt, String notes, List<ReceiptItemRow> items) {
    }

    public record AdjustmentRequest(
            @NotNull UUID productId,
            @NotNull @Min(-100000) @Max(100000) Integer delta,
            @NotNull Reason reason,
            @Size(max = 500) String notes) {
    }

    public record AdjustmentRow(
            UUID id, UUID productId, String productName, int delta, Reason reason, String notes, String createdBy,
            Instant createdAt) {
    }

    public record InventoryRow(
            UUID productId, String name, String brand, int stock, int lowStockThreshold, BigDecimal costPrice,
            BigDecimal sellPrice, BigDecimal stockValue, StockLevel level, boolean active) {
    }

    public enum StockLevel {
        OK,
        LOW,
        OUT
    }

    /** Products with no cost entered are left out of the two money figures. */
    public record InventorySummary(long products, long lowStock, long outOfStock, BigDecimal stockValue,
            BigDecimal potentialProfit) {
    }
}
