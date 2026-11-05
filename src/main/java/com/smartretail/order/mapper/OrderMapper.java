package com.smartretail.order.mapper;

import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.order.dto.response.AddressSnapshot;
import com.smartretail.order.dto.response.AdminOrderRow;
import com.smartretail.order.dto.response.OrderDetail;
import com.smartretail.order.dto.response.OrderItemResponse;
import com.smartretail.order.dto.response.OrderSummary;
import com.smartretail.order.entity.Order;
import com.smartretail.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

@Mapper(config = MapStructConfig.class)
public abstract class OrderMapper {

    @Autowired
    protected JsonMapper json;

    /** Needs an open transaction: reads the order's items. */
    @Mapping(target = "deliveryCharge", source = "shippingCharge")
    @Mapping(target = "address", source = "shippingAddress", qualifiedByName = "readAddress")
    public abstract OrderDetail toDetail(Order order);

    public abstract OrderSummary toSummary(Order order);

    public abstract AdminOrderRow toRow(Order order);

    public abstract OrderItemResponse toItem(OrderItem item);

    @Named("readAddress")
    protected AddressSnapshot readAddress(String shippingAddress) {
        return shippingAddress == null ? null : json.readValue(shippingAddress, AddressSnapshot.class);
    }
}
