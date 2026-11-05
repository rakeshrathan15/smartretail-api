package com.smartretail.inventory.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ReceiptItemRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) @Max(100000) Integer quantity,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99")
        @Digits(integer = 8, fraction = 2, message = "must be in rupees and paise") BigDecimal costPrice) {
}
