package com.smartretail.order.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record Quote(
        List<QuoteLine> lines,
        int totalCases,
        BigDecimal subtotal,
        String couponCode,
        BigDecimal couponDiscount,
        /** Why the entered coupon does not apply, or null. */
        String couponProblem,
        BigDecimal deliveryCharge,
        BigDecimal freeDeliveryAbove,
        /** How much more to add for free delivery, or null when delivery is already free or not offered. */
        BigDecimal amountForFreeDelivery,
        BigDecimal total,
        BigDecimal minOrderValue,
        boolean storeOpen,
        String upiId,
        /** Present when the shop can order on credit. */
        CreditInfo credit,
        /** Everything that stops the order from being placed right now. */
        List<String> problems,
        boolean canPlaceOrder) {
}
