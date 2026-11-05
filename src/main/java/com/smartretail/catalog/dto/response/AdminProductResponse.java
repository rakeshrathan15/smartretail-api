package com.smartretail.catalog.dto.response;

import java.math.BigDecimal;

/** Admin view: adds cost, stock and status fields that customers never see. */
public record AdminProductResponse(
        ProductDetailResponse product,
        BigDecimal costPrice,
        int stockQuantity,
        int lowStockThreshold,
        String barcode,
        boolean active) {
}
