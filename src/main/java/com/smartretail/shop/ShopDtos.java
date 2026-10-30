package com.smartretail.shop;

import com.smartretail.shop.ShopPayment.Method;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ShopDtos {

    private ShopDtos() {
    }

    public record CreditRequest(
            @NotNull UUID userId,
            @Size(max = 120) String shopName,
            @NotNull @DecimalMin("0.00") BigDecimal creditLimit) {
    }

    public record ShopRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 20) String phone,
            @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "must be a valid 15-character GSTIN") String gstin,
            @Size(max = 300) String address,
            @Size(max = 80) String city,
            @Pattern(regexp = "^$|^\\d{6}$", message = "must be a 6-digit PIN code") String pincode,
            UUID deliveryRouteId,
            @NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") BigDecimal creditLimit) {
    }

    public record ActiveRequest(boolean active) {
    }

    public record PaymentRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be more than zero") @DecimalMax("9999999999.99")
            @Digits(integer = 10, fraction = 2, message = "must be in rupees and paise") BigDecimal amount,
            @NotNull Method method,
            @Size(max = 100) String reference) {
    }

    public record ShopRow(
            UUID id, UUID ownerUserId, String name, String phone, String gstin, String address, String city,
            String pincode, UUID routeId, String routeName, BigDecimal creditLimit, BigDecimal outstanding,
            boolean overLimit, boolean active) {
    }

    public record ShopSummary(long totalShops, long activeShops, BigDecimal creditOutstanding, long overLimit) {
    }

    public record CustomerOption(
            UUID userId, String fullName, String shopName, String phone, String email, BigDecimal creditLimit) {
    }

    public record PaymentRow(
            UUID id, BigDecimal amount, Method method, String reference, String receivedBy, Instant createdAt,
            boolean voided, Instant voidedAt, String voidedBy) {
    }

    public record OpenOrder(
            UUID id, String orderNumber, Instant createdAt, String status, BigDecimal total, BigDecimal due) {
    }

    public record Ledger(
            ShopRow shop, BigDecimal creditOrdersTotal, BigDecimal paidTotal, List<OpenOrder> openOrders,
            List<PaymentRow> payments) {
    }
}
