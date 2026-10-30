package com.smartretail.picklist;

import com.smartretail.common.ApiException;
import com.smartretail.common.RouteFilter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What to load on a route's van for a delivery date: every product across the route's orders,
 * added up. Cancelled and returned orders are left out.
 */
@Service
@Transactional(readOnly = true)
public class PickListService {

    public record PickOrder(
            UUID id, String orderNumber, String shopName, String customerPhone, String status, String paymentMethod,
            int totalCases, BigDecimal total) {
    }

    public record PickItem(
            UUID productId, String productName, String brand, int unitsPerCase, int cases, int units,
            BigDecimal value) {
    }

    public record Totals(int orders, int uniqueSkus, int cases, int units, BigDecimal itemsValue, BigDecimal revenue) {
    }

    public record PickList(
            UUID routeId, String routeName, LocalDate date, List<PickOrder> orders, List<PickItem> items,
            Totals totals) {
    }

    private final JdbcClient jdbc;

    public PickListService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PickList forRoute(RouteFilter route, LocalDate date) {
        if (route instanceof RouteFilter.Any) {
            throw ApiException.badRequest("Choose a route, or unassigned");
        }
        UUID routeId = route.routeId().orElse(null);
        String routeName = routeId == null ? "No route" : jdbc.sql("SELECT name FROM delivery_routes WHERE id = :id")
                .param("id", routeId).query(String.class).optional()
                .orElseThrow(() -> ApiException.notFound("Route not found"));

        String orderFilter = " o.delivery_date = :date AND o.status NOT IN ('CANCELLED', 'RETURNED') AND "
                + (routeId == null ? "o.delivery_route_id IS NULL" : "o.delivery_route_id = :routeId");

        List<PickOrder> orders = jdbc.sql("""
                        SELECT o.id, o.order_number, o.shop_name, o.customer_phone, o.status, o.payment_method,
                               o.total_cases, o.total
                        FROM orders o WHERE """ + orderFilter + " ORDER BY o.shop_name NULLS LAST, o.order_number")
                .param("date", date).param("routeId", routeId)
                .query((rs, i) -> new PickOrder(rs.getObject("id", UUID.class), rs.getString("order_number"),
                        rs.getString("shop_name"), rs.getString("customer_phone"), rs.getString("status"),
                        rs.getString("payment_method"), rs.getInt("total_cases"), rs.getBigDecimal("total")))
                .list();

        // Items are grouped by product; a product deleted since keeps its name from the order line.
        List<PickItem> items = jdbc.sql("""
                        SELECT oi.product_id, oi.product_name, p.brand, coalesce(p.units_per_case, 1) AS units_per_case,
                               sum(oi.quantity) AS cases, sum(oi.quantity * coalesce(p.units_per_case, 1)) AS units,
                               sum(oi.total) AS value
                        FROM order_items oi
                        JOIN orders o ON o.id = oi.order_id
                        LEFT JOIN products p ON p.id = oi.product_id
                        WHERE """ + orderFilter + """

                        GROUP BY oi.product_id, oi.product_name, p.brand, p.units_per_case
                        ORDER BY p.brand NULLS LAST, oi.product_name
                        """)
                .param("date", date).param("routeId", routeId)
                .query((rs, i) -> new PickItem(rs.getObject("product_id", UUID.class), rs.getString("product_name"),
                        rs.getString("brand"), rs.getInt("units_per_case"), rs.getInt("cases"), rs.getInt("units"),
                        rs.getBigDecimal("value")))
                .list();

        Totals totals = new Totals(orders.size(), items.size(),
                items.stream().mapToInt(PickItem::cases).sum(),
                items.stream().mapToInt(PickItem::units).sum(),
                items.stream().map(PickItem::value).reduce(BigDecimal.ZERO, BigDecimal::add),
                orders.stream().map(PickOrder::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new PickList(routeId, routeName, date, orders, items, totals);
    }
}
