package com.smartretail.order.dto.response;

import com.smartretail.order.enums.OrderEnums.PaymentMethod;
import com.smartretail.order.enums.OrderEnums.PaymentStatus;
import com.smartretail.order.enums.OrderEnums.PaymentTerms;
import com.smartretail.order.enums.OrderEnums.Status;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderRow(
        UUID id,
        String orderNumber,
        Instant createdAt,
        String shopName,
        String customerName,
        String customerPhone,
        Status status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        PaymentTerms paymentTerms,
        BigDecimal total,
        int totalCases) {
}
