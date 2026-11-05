package com.smartretail.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        UUID categoryId,
        @Size(max = 80) String brand,
        @Size(max = 80) String flavor,
        @Positive Integer unitVolumeMl,
        @NotNull @Positive Integer unitsPerCase,
        @Size(max = 40) String weight,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @DecimalMin("0.00") BigDecimal originalPrice,
        @DecimalMin("0.00") BigDecimal costPrice,
        @PositiveOrZero Integer stockQuantity,
        @PositiveOrZero Integer lowStockThreshold,
        @Size(max = 64) String barcode,
        @Size(max = 30) String badge,
        @Size(max = 4000) String description,
        @Size(max = 4000) String ingredients,
        Boolean active) {
}
