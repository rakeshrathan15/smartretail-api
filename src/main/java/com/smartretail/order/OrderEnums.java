package com.smartretail.order;

/** Values of the CHECK-constrained text columns on {@code orders}. */
public final class OrderEnums {

    private OrderEnums() {
    }

    public enum Channel {
        ONLINE,
        POS
    }

    public enum Status {
        PLACED,
        CONFIRMED,
        PROCESSING,
        PACKED,
        SHIPPED,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED,
        RETURNED
    }

    public enum PaymentMethod {
        UPI,
        COD,
        CREDIT,
        CASH
    }

    public enum PaymentStatus {
        PENDING,
        /** UPI paid by the shop, waiting for the admin to check the screenshot. */
        PENDING_VERIFICATION,
        PAID,
        FAILED,
        REFUNDED
    }

    public enum PaymentTerms {
        PREPAID,
        CREDIT
    }
}
