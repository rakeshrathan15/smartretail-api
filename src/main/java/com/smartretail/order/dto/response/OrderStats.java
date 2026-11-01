package com.smartretail.order.dto.response;

import java.math.BigDecimal;

/** The cards at the top of the orders screen. "Today" is the day in India. */
public record OrderStats(
        long todayOrders,
        BigDecimal todayRevenue,
        long awaitingConfirmation,
        long upiToVerify,
        BigDecimal codToCollect) {
}
