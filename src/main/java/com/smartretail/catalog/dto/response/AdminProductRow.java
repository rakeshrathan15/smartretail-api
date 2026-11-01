package com.smartretail.catalog.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** A row in the admin product table. */
public record AdminProductRow(
        UUID id,
        String slug,
        String name,
        String brand,
        String categoryName,
        int unitsPerCase,
        BigDecimal price,
        BigDecimal lowestPrice,
        String imageUrl,
        int stockQuantity,
        int lowStockThreshold,
        boolean active) {
}
