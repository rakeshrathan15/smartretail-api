package com.smartretail.inventory.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiptRow(
        UUID id, LocalDate receiptDate, String supplierName, String storeName, String invoiceNumber,
        BigDecimal totalCost, int itemCount, int totalCases, String createdBy, Instant createdAt) {
}
