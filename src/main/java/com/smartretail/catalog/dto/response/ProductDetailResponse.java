package com.smartretail.catalog.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String slug,
        String name,
        String brand,
        String flavor,
        CategoryResponse category,
        Integer unitVolumeMl,
        int unitsPerCase,
        String weight,
        BigDecimal price,
        BigDecimal originalPrice,
        BigDecimal lowestPrice,
        boolean inStock,
        String badge,
        String description,
        String ingredients,
        List<ImageResponse> images,
        List<SlabResponse> slabs) {
}
