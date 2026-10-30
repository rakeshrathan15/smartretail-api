package com.smartretail.order;

import com.smartretail.order.OrderDtos.AddressSnapshot;
import com.smartretail.order.OrderDtos.OrderDetail;
import com.smartretail.order.OrderDtos.OrderItemResponse;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class OrderMapper {

    private final JsonMapper json;

    OrderMapper(JsonMapper json) {
        this.json = json;
    }

    /** Needs an open transaction: reads the order's items. */
    OrderDetail toDetail(Order o) {
        AddressSnapshot address = o.getShippingAddress() == null
                ? null
                : json.readValue(o.getShippingAddress(), AddressSnapshot.class);
        return new OrderDetail(o.getId(), o.getOrderNumber(), o.getStatus(), o.getPaymentMethod(), o.getPaymentStatus(),
                o.getPaymentTerms(), o.getSubtotal(), o.getCouponCode(), o.getDiscount(), o.getShippingCharge(),
                o.getTotal(), o.getTotalCases(), address, o.getNotes(), o.getCreatedAt(), o.getDeliveryDate(),
                o.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
