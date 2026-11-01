package com.smartretail.order.dto.response;

import com.smartretail.order.enums.OrderEnums.PaymentMethod;
import com.smartretail.order.enums.OrderEnums.PaymentStatus;
import com.smartretail.order.enums.OrderEnums.PaymentTerms;
import com.smartretail.order.enums.OrderEnums.Status;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrderDetail(
        UUID id,
        String orderNumber,
        Status status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        PaymentTerms paymentTerms,
        BigDecimal subtotal,
        String couponCode,
        BigDecimal discount,
        BigDecimal deliveryCharge,
        BigDecimal total,
        int totalCases,
        AddressSnapshot address,
        String notes,
        Instant createdAt,
        LocalDate deliveryDate,
        List<OrderItemResponse> items) {
}
