package com.smartretail.shop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreditRequest(
        @NotNull UUID userId,
        @Size(max = 120) String shopName,
        @NotNull @DecimalMin("0.00") BigDecimal creditLimit) {
}
