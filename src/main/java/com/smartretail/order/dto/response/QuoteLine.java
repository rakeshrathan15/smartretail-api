package com.smartretail.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteLine(
        UUID productId,
        String slug,
        String name,
        String brand,
        String imageUrl,
        int unitsPerCase,
        int quantity,
        BigDecimal basePrice,
        BigDecimal pricePerCase,
        BigDecimal lineTotal,
        BigDecimal saving,
        int stockAvailable,
        /** Why this line blocks the order (unavailable, not enough stock), or null. */
        String problem) {
}
