package com.smartretail.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CartItem(@NotNull UUID productId, @NotNull @Positive @Max(9999) Integer quantity) {
}
