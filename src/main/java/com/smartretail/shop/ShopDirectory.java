package com.smartretail.shop;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.ApiException;
import com.smartretail.common.SearchTerms;
import com.smartretail.shop.ShopDtos.ShopRow;
import com.smartretail.shop.ShopDtos.ShopSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of shops with what each one owes, for the admin. */
@Repository
@Transactional(readOnly = true)
public class ShopDirectory {

    public enum Standing {
        ALL,
        OWING,
        OVER_LIMIT
    }

    private static final String OVER_LIMIT = "b.outstanding > 0 AND b.outstanding > s.credit_limit";

    private static final String SELECT = """
            SELECT s.id, s.owner_user_id, s.name, coalesce(s.phone, u.phone) AS phone, s.gstin, s.address_line1,
                   s.city, s.pincode, s.delivery_route_id, dr.name AS route_name, s.credit_limit, b.outstanding,
                   s.is_active, (%s) AS over_limit
            """.formatted(OVER_LIMIT) + """
            FROM shops s
            JOIN shop_balances b ON b.shop_id = s.id
            LEFT JOIN users u ON u.id = s.owner_user_id
            LEFT JOIN delivery_routes dr ON dr.id = s.delivery_route_id
            """;

    private final JdbcClient jdbc;

    public ShopDirectory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<ShopRow> find(String search, Standing standing, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE TRUE");

        String pattern = SearchTerms.containsPattern(search);
        if (pattern != null) {
            where.append(" AND (lower(s.name) LIKE :pattern OR coalesce(s.phone, u.phone, '') LIKE :pattern"
                    + " OR lower(coalesce(s.city, '')) LIKE :pattern)");
            params.put("pattern", pattern);
        }
        switch (standing) {
            case ALL -> {
            }
            case OWING -> where.append(" AND b.outstanding > 0");
            case OVER_LIMIT -> where.append(" AND ").append(OVER_LIMIT);
        }

        long total = jdbc.sql("""
                        SELECT count(*) FROM shops s
                        JOIN shop_balances b ON b.shop_id = s.id
                        LEFT JOIN users u ON u.id = s.owner_user_id
                        """ + where)
                .params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<ShopRow> rows = jdbc.sql(SELECT + where + " ORDER BY lower(s.name), s.id LIMIT :limit OFFSET :offset")
                .params(params).query((rs, i) -> toRow(rs)).list();
        return PageResponse.of(rows, page, size, total);
    }

    public ShopRow get(UUID id) {
        return jdbc.sql(SELECT + " WHERE s.id = :id").param("id", id).query((rs, i) -> toRow(rs)).optional()
                .orElseThrow(() -> ApiException.notFound("Shop not found"));
    }

    /** Advance payments do not reduce the total owed by other shops. */
    public ShopSummary summary() {
        return jdbc.sql("""
                        SELECT count(*) AS total, count(*) FILTER (WHERE s.is_active) AS active,
                               coalesce(sum(greatest(b.outstanding, 0)), 0) AS outstanding,
                               count(*) FILTER (WHERE %s) AS over_limit
                        FROM shops s JOIN shop_balances b ON b.shop_id = s.id
                        """.formatted(OVER_LIMIT))
                .query((rs, i) -> new ShopSummary(rs.getLong("total"), rs.getLong("active"),
                        rs.getBigDecimal("outstanding"), rs.getLong("over_limit")))
                .single();
    }

    private static ShopRow toRow(ResultSet rs) throws SQLException {
        return new ShopRow(rs.getObject("id", UUID.class), rs.getObject("owner_user_id", UUID.class),
                rs.getString("name"), rs.getString("phone"), rs.getString("gstin"), rs.getString("address_line1"),
                rs.getString("city"), rs.getString("pincode"), rs.getObject("delivery_route_id", UUID.class),
                rs.getString("route_name"), rs.getBigDecimal("credit_limit"), rs.getBigDecimal("outstanding"),
                rs.getBoolean("over_limit"), rs.getBoolean("is_active"));
    }
}
