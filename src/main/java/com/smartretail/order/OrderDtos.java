package com.smartretail.order;

import com.smartretail.order.OrderEnums.PaymentMethod;
import com.smartretail.order.OrderEnums.PaymentStatus;
import com.smartretail.order.OrderEnums.PaymentTerms;
import com.smartretail.order.OrderEnums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response bodies for checkout and orders. Money is rupees. */
public final class OrderDtos {

    private OrderDtos() {
    }

    // ---- Requests --------------------------------------------------------

    public record CartItem(@NotNull UUID productId, @NotNull @Positive @Max(9999) Integer quantity) {
    }

    /** The phone sends only products and case counts; every price is worked out here. */
    public record QuoteRequest(@NotNull @Size(max = 100) @Valid List<CartItem> items, @Size(max = 40) String couponCode) {
    }

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

    // ---- Quote -----------------------------------------------------------

    public record QuoteLine(
            UUID productId,
            String slug,
            String name,
            String brand,
            String imageUrl,
            int unitsPerCase,
            int quantity,
            BigDecimal basePrice,
            BigDecimal pricePerCase,
            BigDecimal lineTotal,
            BigDecimal saving,
            int stockAvailable,
            /** Why this line blocks the order (unavailable, not enough stock), or null. */
            String problem) {
    }

    public record CreditInfo(BigDecimal limit, BigDecimal outstanding, BigDecimal available) {
    }

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

    // ---- Orders ----------------------------------------------------------

    public record AddressSnapshot(
            String label, String fullName, String phone, String addressLine1, String addressLine2,
            String city, String state, String pincode) {
    }

    public record OrderItemResponse(UUID productId, String productName, int quantity, BigDecimal price, BigDecimal total) {

        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getQuantity(), item.getPrice(),
                    item.getTotal());
        }
    }

    public record OrderSummary(
            UUID id,
            String orderNumber,
            Status status,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            BigDecimal total,
            int totalCases,
            Instant createdAt) {

        static OrderSummary from(Order o) {
            return new OrderSummary(o.getId(), o.getOrderNumber(), o.getStatus(), o.getPaymentMethod(),
                    o.getPaymentStatus(), o.getTotal(), o.getTotalCases(), o.getCreatedAt());
        }
    }

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
            List<OrderItemResponse> items) {
    }
}
