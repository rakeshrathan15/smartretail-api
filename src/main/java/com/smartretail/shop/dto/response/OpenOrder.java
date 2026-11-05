package com.smartretail.shop.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OpenOrder(
        UUID id, String orderNumber, Instant createdAt, String status, BigDecimal total, BigDecimal due) {
}
