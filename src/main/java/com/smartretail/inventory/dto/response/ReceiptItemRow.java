package com.smartretail.inventory.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ReceiptItemRow(UUID productId, String productName, int quantity, BigDecimal costPrice,
        BigDecimal total) {
}
