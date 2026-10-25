package com.smartretail.order;

import com.smartretail.order.OrderDtos.OrderDetail;
import com.smartretail.order.OrderEnums.PaymentMethod;
import com.smartretail.order.OrderEnums.PaymentStatus;
import com.smartretail.order.OrderEnums.PaymentTerms;
import com.smartretail.order.OrderEnums.Status;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Bodies for the admin orders API. */
public final class AdminOrderDtos {

    private AdminOrderDtos() {
    }

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

        static AdminOrderRow from(Order o) {
            return new AdminOrderRow(o.getId(), o.getOrderNumber(), o.getCreatedAt(), o.getShopName(),
                    o.getCustomerName(), o.getCustomerPhone(), o.getStatus(), o.getPaymentMethod(),
                    o.getPaymentStatus(), o.getPaymentTerms(), o.getTotal(), o.getTotalCases());
        }
    }

    /** The cards at the top of the orders screen. "Today" is the day in India. */
    public record OrderStats(
            long todayOrders,
            BigDecimal todayRevenue,
            long awaitingConfirmation,
            long upiToVerify,
            BigDecimal codToCollect) {
    }

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

    public record StatusChange(@NotNull Status status) {
    }

    public enum PaymentDecision {
        APPROVE,
        REJECT
    }

    public record PaymentReview(@NotNull PaymentDecision decision) {
    }
}
