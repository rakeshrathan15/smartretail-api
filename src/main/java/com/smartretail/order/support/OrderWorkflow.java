package com.smartretail.order.support;

import static com.smartretail.order.enums.OrderEnums.Status.CANCELLED;
import static com.smartretail.order.enums.OrderEnums.Status.CONFIRMED;
import static com.smartretail.order.enums.OrderEnums.Status.DELIVERED;
import static com.smartretail.order.enums.OrderEnums.Status.OUT_FOR_DELIVERY;
import static com.smartretail.order.enums.OrderEnums.Status.PACKED;
import static com.smartretail.order.enums.OrderEnums.Status.PLACED;
import static com.smartretail.order.enums.OrderEnums.Status.PROCESSING;
import static com.smartretail.order.enums.OrderEnums.Status.RETURNED;
import static com.smartretail.order.enums.OrderEnums.Status.SHIPPED;

import com.smartretail.order.enums.OrderEnums.Status;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Which status an order can move to next. Orders only move forward; cancelling is possible until
 * the order leaves the warehouse, and a delivered order can be marked returned.
 */
public final class OrderWorkflow {

    private static final Map<Status, List<Status>> NEXT = new EnumMap<>(Status.class);

    static {
        NEXT.put(PLACED, List.of(CONFIRMED, CANCELLED));
        NEXT.put(CONFIRMED, List.of(PROCESSING, PACKED, CANCELLED));
        NEXT.put(PROCESSING, List.of(PACKED, CANCELLED));
        NEXT.put(PACKED, List.of(OUT_FOR_DELIVERY, SHIPPED, CANCELLED));
        NEXT.put(SHIPPED, List.of(OUT_FOR_DELIVERY, DELIVERED));
        NEXT.put(OUT_FOR_DELIVERY, List.of(DELIVERED, RETURNED));
        NEXT.put(DELIVERED, List.of(RETURNED));
        NEXT.put(CANCELLED, List.of());
        NEXT.put(RETURNED, List.of());
    }

    private OrderWorkflow() {
    }

    public static List<Status> nextStatuses(Status current) {
        return NEXT.get(current);
    }

    public static boolean canMove(Status from, Status to) {
        return NEXT.get(from).contains(to);
    }

    /** Cancelled and returned orders give their cases back to stock. */
    public static boolean returnsStock(Status status) {
        return status == CANCELLED || status == RETURNED;
    }
}
