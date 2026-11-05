package com.smartretail.order.dto.response;

import com.smartretail.order.enums.OrderEnums.PaymentMethod;
import com.smartretail.order.enums.OrderEnums.PaymentStatus;
import com.smartretail.order.enums.OrderEnums.Status;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummary(
        UUID id,
        String orderNumber,
        Status status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal total,
        int totalCases,
        Instant createdAt) {
}
