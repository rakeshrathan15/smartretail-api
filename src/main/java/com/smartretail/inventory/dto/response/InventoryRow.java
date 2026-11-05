package com.smartretail.inventory.dto.response;

import com.smartretail.inventory.dto.request.StockLevel;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryRow(
        UUID productId, String name, String brand, int stock, int lowStockThreshold, BigDecimal costPrice,
        BigDecimal sellPrice, BigDecimal stockValue, StockLevel level, boolean active) {
}
