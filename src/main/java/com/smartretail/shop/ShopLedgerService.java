package com.smartretail.shop;

import com.smartretail.common.ApiException;
import com.smartretail.shop.ShopDtos.Ledger;
import com.smartretail.shop.ShopDtos.OpenOrder;
import com.smartretail.shop.ShopDtos.PaymentRequest;
import com.smartretail.shop.ShopDtos.PaymentRow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Money shops pay back: recording and voiding repayments, and each shop's statement. */
@Service
@Transactional
public class ShopLedgerService {

    private static final int RECENT_PAYMENTS = 100;

    private record CreditOrder(UUID id, String orderNumber, OffsetDateTime createdAt, String status,
            BigDecimal total) {
    }

    private final ShopRepository shops;
    private final ShopPaymentRepository payments;
    private final ShopDirectory directory;
    private final JdbcClient jdbc;
    private final Clock clock = Clock.systemUTC();

    public ShopLedgerService(ShopRepository shops, ShopPaymentRepository payments, ShopDirectory directory,
            JdbcClient jdbc) {
        this.shops = shops;
        this.payments = payments;
        this.directory = directory;
        this.jdbc = jdbc;
    }

    /** A shop that was turned off can still pay what it owes. More than it owes counts as an advance. */
    public Ledger recordPayment(UUID shopId, PaymentRequest request, UUID adminId) {
        lockShop(shopId);
        payments.saveAndFlush(new ShopPayment(shopId, request.amount(), request.method(),
                request.reference() == null || request.reference().isBlank() ? null : request.reference().trim(),
                adminId));
        return ledger(shopId);
    }

    /** Takes a wrong entry or bounced cheque out of the balance; it stays in the history. */
    public Ledger voidPayment(UUID shopId, UUID paymentId, UUID adminId) {
        lockShop(shopId);
        ShopPayment payment = payments.findByIdAndShopId(paymentId, shopId)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));
        if (payment.isVoided()) {
            throw ApiException.conflict("This payment has already been voided");
        }
        payment.voidBy(adminId, clock.instant());
        payments.flush();
        return ledger(shopId);
    }

    @Transactional(readOnly = true)
    public Ledger ledger(UUID shopId) {
        var shop = directory.get(shopId);
        BigDecimal paid = jdbc.sql("SELECT paid_total FROM shop_balances WHERE shop_id = :id")
                .param("id", shopId).query(BigDecimal.class).single();

        List<CreditOrder> orders = jdbc.sql("""
                        SELECT id, order_number, created_at, status, total FROM orders
                        WHERE shop_id = :id AND payment_terms = 'CREDIT' AND status NOT IN ('CANCELLED', 'RETURNED')
                        ORDER BY created_at, order_number
                        """)
                .param("id", shopId)
                .query((rs, i) -> new CreditOrder(rs.getObject("id", UUID.class), rs.getString("order_number"),
                        rs.getObject("created_at", OffsetDateTime.class), rs.getString("status"),
                        rs.getBigDecimal("total")))
                .list();
        List<BigDecimal> dues = CreditAllocation.dues(orders.stream().map(CreditOrder::total).toList(), paid);
        List<OpenOrder> open = new ArrayList<>();
        BigDecimal ordersTotal = BigDecimal.ZERO;
        for (int i = 0; i < orders.size(); i++) {
            CreditOrder order = orders.get(i);
            ordersTotal = ordersTotal.add(order.total());
            if (dues.get(i).signum() > 0) {
                open.add(new OpenOrder(order.id(), order.orderNumber(), order.createdAt().toInstant(), order.status(),
                        order.total(), dues.get(i)));
            }
        }
        return new Ledger(shop, ordersTotal, paid, open, recentPayments(shopId));
    }

    private List<PaymentRow> recentPayments(UUID shopId) {
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

    /** One change at a time per shop, and the 404 for a shop that does not exist. */
    private void lockShop(UUID shopId) {
        shops.lockById(shopId).orElseThrow(() -> ApiException.notFound("Shop not found"));
    }
}
