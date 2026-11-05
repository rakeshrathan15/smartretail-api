package com.smartretail.order.dto.response;

import com.smartretail.order.enums.OrderEnums.Status;
import java.math.BigDecimal;
import java.util.Map;

/** The cards at the top of the orders screen. "Today" is the day in India. */
public record OrderStats(
        long todayOrders,
        BigDecimal todayRevenue,
        long awaitingConfirmation,
        long upiToVerify,
        BigDecimal codToCollect,
        /** Every order so far, by status. A status with no orders is left out. */
        Map<Status, Long> statusCounts) {
}
