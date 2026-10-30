package com.smartretail.inventory;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.ApiException;
import com.smartretail.common.SearchTerms;
import com.smartretail.inventory.InventoryDtos.AdjustmentRow;
import com.smartretail.inventory.InventoryDtos.InventoryRow;
import com.smartretail.inventory.InventoryDtos.InventorySummary;
import com.smartretail.inventory.InventoryDtos.ReceiptDetail;
import com.smartretail.inventory.InventoryDtos.ReceiptItemRow;
import com.smartretail.inventory.InventoryDtos.ReceiptRow;
import com.smartretail.inventory.InventoryDtos.StockLevel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Read-only views of stock, receipts and adjustments for the admin. */
@Repository
@Transactional(readOnly = true)
public class InventoryDirectory {

    public enum LevelFilter {
        ALL,
        LOW,
        OUT
    }

    private static final String LOW = "p.stock_quantity > 0 AND p.stock_quantity <= p.low_stock_threshold";

    private static final String RECEIPT_SELECT = """
            SELECT r.id, r.receipt_date, sup.name AS supplier_name, st.name AS store_name, r.invoice_number,
                   r.total_cost, r.created_at, r.notes,
                   coalesce(pr.full_name, u.email, u.phone) AS created_by,
                   (SELECT count(*) FROM stock_receipt_items i WHERE i.receipt_id = r.id) AS item_count,
                   (SELECT coalesce(sum(i.quantity), 0) FROM stock_receipt_items i WHERE i.receipt_id = r.id) AS total_cases
            FROM stock_receipts r
            LEFT JOIN suppliers sup ON sup.id = r.supplier_id
            LEFT JOIN stores st ON st.id = r.store_id
            LEFT JOIN users u ON u.id = r.created_by
            LEFT JOIN profiles pr ON pr.user_id = r.created_by
            """;

    private final JdbcClient jdbc;

    public InventoryDirectory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<InventoryRow> stock(String search, LevelFilter level, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE TRUE");
        String pattern = SearchTerms.containsPattern(search);
        if (pattern != null) {
            where.append(" AND (lower(p.name) LIKE :pattern OR lower(coalesce(p.brand, '')) LIKE :pattern)");
            params.put("pattern", pattern);
        }
        switch (level) {
            case ALL -> {
            }
            case LOW -> where.append(" AND ").append(LOW);
            case OUT -> where.append(" AND p.stock_quantity <= 0");
        }
        long total = jdbc.sql("SELECT count(*) FROM products p" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<InventoryRow> rows = jdbc.sql("""
                        SELECT p.id, p.name, p.brand, p.stock_quantity, p.low_stock_threshold, p.cost_price, p.price,
                               p.is_active, p.cost_price * p.stock_quantity AS stock_value
                        FROM products p""" + where + " ORDER BY lower(p.name), p.id LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> {
                    int stock = rs.getInt("stock_quantity");
                    int threshold = rs.getInt("low_stock_threshold");
                    StockLevel stockLevel = stock <= 0 ? StockLevel.OUT : stock <= threshold ? StockLevel.LOW : StockLevel.OK;
                    return new InventoryRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("brand"),
                            stock, threshold, rs.getBigDecimal("cost_price"), rs.getBigDecimal("price"),
                            rs.getBigDecimal("stock_value"), stockLevel, rs.getBoolean("is_active"));
                })
                .list();
        return PageResponse.of(rows, page, size, total);
    }

    public InventorySummary summary() {
        return jdbc.sql("""
                        SELECT count(*) AS products,
                               count(*) FILTER (WHERE %s) AS low_stock,
                               count(*) FILTER (WHERE p.stock_quantity <= 0) AS out_of_stock,
                               coalesce(sum(p.cost_price * p.stock_quantity), 0) AS stock_value,
                               coalesce(sum((p.price - p.cost_price) * p.stock_quantity), 0) AS potential_profit
                        FROM products p
                        """.formatted(LOW))
                .query((rs, i) -> new InventorySummary(rs.getLong("products"), rs.getLong("low_stock"),
                        rs.getLong("out_of_stock"), rs.getBigDecimal("stock_value"),
                        rs.getBigDecimal("potential_profit")))
                .single();
    }

    public PageResponse<AdjustmentRow> adjustments(UUID productId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String where = "";
        if (productId != null) {
            where = " WHERE a.product_id = :productId";
            params.put("productId", productId);
        }
        long total = jdbc.sql("SELECT count(*) FROM stock_adjustments a" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<AdjustmentRow> rows = jdbc.sql("""
                        SELECT a.id, a.product_id, p.name AS product_name, a.delta, a.reason, a.notes, a.created_at,
                               coalesce(pr.full_name, u.email, u.phone) AS created_by
                        FROM stock_adjustments a
                        JOIN products p ON p.id = a.product_id
                        LEFT JOIN users u ON u.id = a.created_by
                        LEFT JOIN profiles pr ON pr.user_id = a.created_by
                        """ + where + " ORDER BY a.created_at DESC, a.id LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, i) -> new AdjustmentRow(rs.getObject("id", UUID.class),
                        rs.getObject("product_id", UUID.class), rs.getString("product_name"), rs.getInt("delta"),
                        StockAdjustment.Reason.valueOf(rs.getString("reason")), rs.getString("notes"),
                        rs.getString("created_by"), rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
        return PageResponse.of(rows, page, size, total);
    }

    public PageResponse<ReceiptRow> receipts(UUID supplierId, LocalDate from, LocalDate to, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" WHERE TRUE");
        if (supplierId != null) {
            where.append(" AND r.supplier_id = :supplierId");
            params.put("supplierId", supplierId);
        }
        if (from != null) {
            where.append(" AND r.receipt_date >= :from");
            params.put("from", from);
        }
        if (to != null) {
            where.append(" AND r.receipt_date <= :to");
            params.put("to", to);
        }
        long total = jdbc.sql("SELECT count(*) FROM stock_receipts r" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<ReceiptRow> rows = jdbc.sql(RECEIPT_SELECT + where
                        + " ORDER BY r.receipt_date DESC, r.created_at DESC, r.id LIMIT :limit OFFSET :offset")
                .params(params).query((rs, i) -> toReceipt(rs)).list();
        return PageResponse.of(rows, page, size, total);
    }

    public ReceiptDetail receipt(UUID id) {
        record Head(ReceiptRow row, String notes) {
        }
        Head head = jdbc.sql(RECEIPT_SELECT + " WHERE r.id = :id").param("id", id)
                .query((rs, i) -> new Head(toReceipt(rs), rs.getString("notes"))).optional()
                .orElseThrow(() -> ApiException.notFound("Receipt not found"));
        List<ReceiptItemRow> items = jdbc.sql("""
                        SELECT i.product_id, p.name, i.quantity, i.cost_price, i.total
                        FROM stock_receipt_items i JOIN products p ON p.id = i.product_id
                        WHERE i.receipt_id = :id ORDER BY lower(p.name)
                        """)
                .param("id", id)
                .query((rs, i) -> new ReceiptItemRow(rs.getObject("product_id", UUID.class), rs.getString("name"),
                        rs.getInt("quantity"), rs.getBigDecimal("cost_price"), rs.getBigDecimal("total")))
                .list();
        return new ReceiptDetail(head.row(), head.notes(), items);
    }

    private static ReceiptRow toReceipt(ResultSet rs) throws SQLException {
        return new ReceiptRow(rs.getObject("id", UUID.class), rs.getObject("receipt_date", LocalDate.class),
                rs.getString("supplier_name"), rs.getString("store_name"), rs.getString("invoice_number"),
                rs.getBigDecimal("total_cost"), rs.getInt("item_count"), rs.getInt("total_cases"),
                rs.getString("created_by"), rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
