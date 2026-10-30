package com.smartretail.customer;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.RouteFilter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Read-only list of registered shop owners for the admin, with their delivery route. */
@Repository
@Transactional(readOnly = true)
public class CustomerDirectory {

    public record CustomerRow(
            UUID userId, String shopName, String fullName, String phone, String email, String city, String pincode,
            boolean hasLocation, boolean profileComplete, UUID routeId, String routeName, String routeColor,
            Instant joinedAt) {
    }

    private static final String FROM = """
            FROM users u
            JOIN user_roles r ON r.user_id = u.id AND r.role = 'CUSTOMER'
            LEFT JOIN profiles p ON p.user_id = u.id
            LEFT JOIN delivery_routes dr ON dr.id = p.delivery_route_id
            """;

    private final JdbcClient jdbc;

    public CustomerDirectory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Newest first. {@code search} matches shop name, owner name, phone, email, city or PIN code. */
    public PageResponse<CustomerRow> find(String search, RouteFilter route, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE TRUE");

        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        if (!term.isEmpty()) {
            where.append("""
                     AND (lower(coalesce(p.shop_name, '')) LIKE :pattern OR lower(coalesce(p.full_name, '')) LIKE :pattern
                          OR coalesce(u.phone, '') LIKE :pattern OR lower(coalesce(u.email, '')) LIKE :pattern
                          OR lower(coalesce(p.shop_city, '')) LIKE :pattern OR coalesce(p.shop_pincode, '') LIKE :pattern)
                    """);
            params.put("pattern", "%" + term + "%");
        }
        switch (route) {
            case RouteFilter.Any any -> {
            }
            case RouteFilter.Unassigned unassigned -> where.append(" AND p.delivery_route_id IS NULL");
            case RouteFilter.Route one -> {
                where.append(" AND p.delivery_route_id = :routeId");
                params.put("routeId", one.id());
            }
        }

        long total = jdbc.sql("SELECT count(*) " + FROM + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<CustomerRow> rows = jdbc.sql("""
                        SELECT u.id, p.shop_name, p.full_name, u.phone, u.email, p.shop_city, p.shop_pincode,
                               (p.shop_lat IS NOT NULL AND p.shop_lng IS NOT NULL) AS has_location,
                               coalesce(p.shop_profile_complete, FALSE) AS profile_complete,
                               dr.id AS route_id, dr.name AS route_name, dr.color AS route_color, u.created_at
                        """ + FROM + where + " ORDER BY u.created_at DESC, u.id LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> new CustomerRow(rs.getObject("id", UUID.class), rs.getString("shop_name"),
                        rs.getString("full_name"), rs.getString("phone"), rs.getString("email"),
                        rs.getString("shop_city"), rs.getString("shop_pincode"), rs.getBoolean("has_location"),
                        rs.getBoolean("profile_complete"), rs.getObject("route_id", UUID.class),
                        rs.getString("route_name"), rs.getString("route_color"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
        return new PageResponse<>(rows, page, size, total, (int) Math.ceil(total / (double) size));
    }
}
