package com.smartretail.catalog.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** A product as shown in lists and grids. */
public record ProductCardResponse(
        UUID id,
        String slug,
        String name,
        String brand,
        UUID categoryId,
        Integer unitVolumeMl,
        int unitsPerCase,
        BigDecimal price,
        BigDecimal originalPrice,
        BigDecimal lowestPrice,
        String imageUrl,
        boolean inStock,
        String badge) {
}
