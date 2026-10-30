-- Credit ledger. A repayment entered by mistake, or a cheque that bounces, is voided rather than
-- deleted, so the history of money received stays complete.
ALTER TABLE shop_payments
    ADD COLUMN voided_at TIMESTAMPTZ,
    ADD COLUMN voided_by UUID REFERENCES users (id) ON DELETE SET NULL;

-- The one definition of what a shop owes: credit orders not cancelled or returned, minus repayments
-- not voided. Negative when the shop has paid in advance.
CREATE VIEW shop_balances AS
SELECT s.id AS shop_id,
       coalesce(o.total, 0) AS credit_orders_total,
       coalesce(p.total, 0) AS paid_total,
       coalesce(o.total, 0) - coalesce(p.total, 0) AS outstanding
FROM shops s
LEFT JOIN (SELECT shop_id, sum(total) AS total
           FROM orders
           WHERE payment_terms = 'CREDIT' AND status NOT IN ('CANCELLED', 'RETURNED')
           GROUP BY shop_id) o ON o.shop_id = s.id
LEFT JOIN (SELECT shop_id, sum(amount) AS total
           FROM shop_payments
           WHERE voided_at IS NULL
           GROUP BY shop_id) p ON p.shop_id = s.id;
