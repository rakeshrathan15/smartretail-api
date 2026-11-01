package com.smartretail.inventory.dto.request;

import com.smartretail.inventory.entity.StockAdjustment.Reason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdjustmentRequest(
        @NotNull UUID productId,
        @NotNull @Min(-100000) @Max(100000) Integer delta,
        @NotNull Reason reason,
        @Size(max = 500) String notes) {
}
