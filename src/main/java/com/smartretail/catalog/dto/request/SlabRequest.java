package com.smartretail.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SlabRequest(
        @NotNull @Positive Integer minQty,
        @Positive Integer maxQty,
        @NotNull @DecimalMin("0.00") BigDecimal pricePerCase) {
}
