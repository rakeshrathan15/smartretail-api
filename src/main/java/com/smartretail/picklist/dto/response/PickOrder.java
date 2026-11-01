package com.smartretail.picklist.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PickOrder(
        UUID id, String orderNumber, String shopName, String customerPhone, String status, String paymentMethod,
        int totalCases, BigDecimal total) {
}
