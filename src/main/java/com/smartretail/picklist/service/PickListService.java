package com.smartretail.picklist.service;

import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.util.RouteFilter;
import com.smartretail.picklist.dto.response.PickItem;
import com.smartretail.picklist.dto.response.PickList;
import com.smartretail.picklist.dto.response.PickOrder;
import com.smartretail.picklist.dto.response.Totals;
import com.smartretail.picklist.repository.PickListDirectory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What to load on a route's van for a delivery date: every product across the route's orders,
 * added up. Cancelled and returned orders are left out.
 */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PickListService {

    private final PickListDirectory directory;

    public PickList forRoute(RouteFilter route, LocalDate date) {
        if (route instanceof RouteFilter.Any) {
            throw new ApiException(ErrorCode.INVALID_ROUTE_FILTER, "Choose a route, or unassigned");
        }
        UUID routeId = route.routeId().orElse(null);
        String routeName = routeId == null ? "No route" : directory.routeName(routeId)
                .orElseThrow(() -> new ApiException(ErrorCode.ROUTE_NOT_FOUND, "Route not found"));

        List<PickOrder> orders = directory.orders(routeId, date);
        List<PickItem> items = directory.items(routeId, date);

        Totals totals = new Totals(orders.size(), items.size(),
                items.stream().mapToInt(PickItem::cases).sum(),
                items.stream().mapToInt(PickItem::units).sum(),
                items.stream().map(PickItem::value).reduce(BigDecimal.ZERO, BigDecimal::add),
                orders.stream().map(PickOrder::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new PickList(routeId, routeName, date, orders, items, totals);
    }
}
