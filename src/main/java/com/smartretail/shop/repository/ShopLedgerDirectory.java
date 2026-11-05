package com.smartretail.shop.repository;

import com.smartretail.shop.dto.response.PaymentRow;
import com.smartretail.shop.entity.ShopPayment;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of what makes up a shop's statement: repayments and credit orders. */
@RequiredArgsConstructor
@Repository
@Transactional(readOnly = true)
public class ShopLedgerDirectory {

    private static final int RECENT_PAYMENTS = 100;

    public record CreditOrder(UUID id, String orderNumber, OffsetDateTime createdAt, String status,
            BigDecimal total) {
    }

    private final JdbcClient jdbc;

    public BigDecimal paidTotal(UUID shopId) {
        return jdbc.sql("SELECT paid_total FROM shop_balances WHERE shop_id = :id")
                .param("id", shopId).query(BigDecimal.class).single();
    }

    public List<CreditOrder> creditOrders(UUID shopId) {
        return jdbc.sql("""
                        SELECT id, order_number, created_at, status, total FROM orders
                        WHERE shop_id = :id AND payment_terms = 'CREDIT' AND status NOT IN ('CANCELLED', 'RETURNED')
                        ORDER BY created_at, order_number
                        """)
                .param("id", shopId)
                .query((rs, i) -> new CreditOrder(rs.getObject("id", UUID.class), rs.getString("order_number"),
                        rs.getObject("created_at", OffsetDateTime.class), rs.getString("status"),
                        rs.getBigDecimal("total")))
                .list();
    }

    public List<PaymentRow> recentPayments(UUID shopId) {
        return jdbc.sql("""
                        SELECT p.id, p.amount, p.method, p.reference, p.created_at, p.voided_at,
                               coalesce(rp.full_name, ru.email, ru.phone) AS received_by,
                               coalesce(vp.full_name, vu.email, vu.phone) AS voided_by
                        FROM shop_payments p
                        LEFT JOIN users ru ON ru.id = p.received_by
                        LEFT JOIN profiles rp ON rp.user_id = p.received_by
                        LEFT JOIN users vu ON vu.id = p.voided_by
                        LEFT JOIN profiles vp ON vp.user_id = p.voided_by
                        WHERE p.shop_id = :id
                        ORDER BY p.created_at DESC, p.id
                        LIMIT :limit
                        """)
                .param("id", shopId).param("limit", RECENT_PAYMENTS)
                .query((rs, i) -> {
                    OffsetDateTime voidedAt = rs.getObject("voided_at", OffsetDateTime.class);
                    return new PaymentRow(rs.getObject("id", UUID.class), rs.getBigDecimal("amount"),
                            ShopPayment.Method.valueOf(rs.getString("method")), rs.getString("reference"),
                            rs.getString("received_by"), rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                            voidedAt != null, voidedAt == null ? null : voidedAt.toInstant(), rs.getString("voided_by"));
                })
                .list();
    }
}
