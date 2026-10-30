package com.smartretail.order;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.ApiException;
import com.smartretail.order.OrderDtos.OrderDetail;
import com.smartretail.order.OrderDtos.OrderSummary;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's own order history. */
@Service
@Transactional(readOnly = true)
public class MyOrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orders;
    private final OrderMapper mapper;

    public MyOrderService(OrderRepository orders, OrderMapper mapper) {
        this.orders = orders;
        this.mapper = mapper;
    }

    public PageResponse<OrderSummary> list(UUID userId, int page, int size) {
        if (page < 0) {
            throw ApiException.badRequest("page must be 0 or more");
        }
        Page<Order> result = orders.findAllByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(page, Math.clamp(size, 1, MAX_PAGE_SIZE)));
        return new PageResponse<>(result.getContent().stream().map(OrderSummary::from).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public OrderDetail get(UUID userId, UUID id) {
        return orders.findByIdAndUserId(id, userId).map(mapper::toDetail)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
    }
}
