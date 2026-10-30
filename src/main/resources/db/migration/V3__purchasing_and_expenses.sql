ALTER TABLE suppliers ADD COLUMN gstin TEXT;

-- The same bill entered twice would count the stock twice.
CREATE UNIQUE INDEX uq_stock_receipts_supplier_invoice
    ON stock_receipts (supplier_id, lower(invoice_number))
    WHERE supplier_id IS NOT NULL AND invoice_number IS NOT NULL;
CREATE INDEX idx_stock_receipts_date ON stock_receipts (receipt_date DESC);
CREATE INDEX idx_stock_adjustments_created ON stock_adjustments (created_at DESC);

ALTER TABLE expenses ALTER COLUMN category SET DEFAULT 'GENERAL';
UPDATE expenses SET category = 'GENERAL';
ALTER TABLE expenses ADD CONSTRAINT expenses_category_check CHECK (category IN
    ('GENERAL', 'RENT', 'SALARIES', 'FUEL', 'UTILITIES', 'MAINTENANCE', 'TRANSPORT', 'MARKETING', 'OTHER'));

ALTER TABLE products ADD CONSTRAINT products_stock_not_negative CHECK (stock_quantity >= 0) NOT VALID;
