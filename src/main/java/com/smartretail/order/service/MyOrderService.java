package com.smartretail.order.service;

import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.response.PageResponse;
import com.smartretail.order.dto.response.OrderDetail;
import com.smartretail.order.dto.response.OrderSummary;
import com.smartretail.order.entity.Order;
import com.smartretail.order.mapper.OrderMapper;
import com.smartretail.order.repository.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's own order history. */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MyOrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orders;
    private final OrderMapper mapper;

    public PageResponse<OrderSummary> list(UUID userId, int page, int size) {
        if (page < 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE, "page must be 0 or more");
        }
        Page<Order> result = orders.findAllByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(page, Math.clamp(size, 1, MAX_PAGE_SIZE)));
        return new PageResponse<>(result.getContent().stream().map(mapper::toSummary).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public OrderDetail get(UUID userId, UUID id) {
        return orders.findByIdAndUserId(id, userId).map(mapper::toDetail)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
    }
}
