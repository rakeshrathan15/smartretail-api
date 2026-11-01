package com.smartretail.order.dto.request;

import com.smartretail.order.enums.OrderEnums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotEmpty @Size(max = 100) @Valid List<CartItem> items,
        @Size(max = 40) String couponCode,
        @NotNull UUID addressId,
        @NotNull PaymentMethod paymentMethod,
        /** Reference returned by POST /api/me/payment-screenshots; required for UPI. */
        @Size(max = 200) String paymentScreenshot,
        @Size(max = 500) String notes,
        /** The total the shop owner saw; if prices changed since, the order is refused so they can review. */
        BigDecimal expectedTotal) {
}
