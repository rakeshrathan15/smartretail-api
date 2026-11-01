package com.smartretail.inventory.dto.response;

import com.smartretail.inventory.entity.StockAdjustment.Reason;
import java.time.Instant;
import java.util.UUID;

public record AdjustmentRow(
        UUID id, UUID productId, String productName, int delta, Reason reason, String notes, String createdBy,
        Instant createdAt) {
}
