package com.smartretail.order.dto.response;

import com.smartretail.order.enums.OrderEnums.Status;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetail(
        OrderDetail order,
        UUID userId,
        UUID shopId,
        String shopName,
        String customerName,
        String customerPhone,
        boolean hasPaymentScreenshot,
        /** The statuses the admin can move this order to now; empty when it is finished. */
        List<Status> nextStatuses,
        /** True when a UPI payment is waiting to be approved or rejected. */
        boolean paymentNeedsReview) {
}
