package com.smartretail.shop.dto.response;

import com.smartretail.shop.entity.ShopPayment.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRow(
        UUID id, BigDecimal amount, Method method, String reference, String receivedBy, Instant createdAt,
        boolean voided, Instant voidedAt, String voidedBy) {
}
