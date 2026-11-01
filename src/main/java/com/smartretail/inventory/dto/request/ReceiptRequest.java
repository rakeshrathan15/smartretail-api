package com.smartretail.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReceiptRequest(
        UUID supplierId,
        UUID storeId,
        @NotNull LocalDate receiptDate,
        @Size(max = 60) String invoiceNumber,
        @Size(max = 500) String notes,
        @NotEmpty @Size(max = 200) List<@Valid ReceiptItemRequest> items) {
}
