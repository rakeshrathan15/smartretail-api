package com.smartretail.picklist.repository;

import com.smartretail.picklist.dto.response.PickItem;
import com.smartretail.picklist.dto.response.PickOrder;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of a delivery date's orders and products. A null route id means orders on no route. */
@RequiredArgsConstructor
@Repository
@Transactional(readOnly = true)
public class PickListDirectory {

    private final JdbcClient jdbc;

    public Optional<String> routeName(UUID routeId) {
        return jdbc.sql("SELECT name FROM delivery_routes WHERE id = :id")
                .param("id", routeId).query(String.class).optional();
    }

    public List<PickOrder> orders(UUID routeId, LocalDate date) {
        return jdbc.sql("""
                        SELECT o.id, o.order_number, o.shop_name, o.customer_phone, o.status, o.payment_method,
                               o.total_cases, o.total
                        FROM orders o WHERE """ + orderFilter(routeId) + " ORDER BY o.shop_name NULLS LAST, o.order_number")
                .param("date", date).param("routeId", routeId)
                .query((rs, i) -> new PickOrder(rs.getObject("id", UUID.class), rs.getString("order_number"),
                        rs.getString("shop_name"), rs.getString("customer_phone"), rs.getString("status"),
                        rs.getString("payment_method"), rs.getInt("total_cases"), rs.getBigDecimal("total")))
                .list();
    }

    /** Items are grouped by product; a product deleted since keeps its name from the order line. */
    public List<PickItem> items(UUID routeId, LocalDate date) {
        return jdbc.sql("""
                        SELECT oi.product_id, oi.product_name, p.brand, coalesce(p.units_per_case, 1) AS units_per_case,
                               sum(oi.quantity) AS cases, sum(oi.quantity * coalesce(p.units_per_case, 1)) AS units,
                               sum(oi.total) AS value
                        FROM order_items oi
                        JOIN orders o ON o.id = oi.order_id
                        LEFT JOIN products p ON p.id = oi.product_id
                        WHERE """ + orderFilter(routeId) + """

                        GROUP BY oi.product_id, oi.product_name, p.brand, p.units_per_case
                        ORDER BY p.brand NULLS LAST, oi.product_name
                        """)
                .param("date", date).param("routeId", routeId)
                .query((rs, i) -> new PickItem(rs.getObject("product_id", UUID.class), rs.getString("product_name"),
                        rs.getString("brand"), rs.getInt("units_per_case"), rs.getInt("cases"), rs.getInt("units"),
                        rs.getBigDecimal("value")))
                .list();
    }

    private static String orderFilter(UUID routeId) {
        return " o.delivery_date = :date AND o.status NOT IN ('CANCELLED', 'RETURNED') AND "
                + (routeId == null ? "o.delivery_route_id IS NULL" : "o.delivery_route_id = :routeId");
    }
}
