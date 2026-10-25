-- Development checkout data. Loaded only by the dev profile.

UPDATE store_settings SET value = 'smartretail@upi' WHERE key = 'upi_id';

INSERT INTO coupons (code, description, discount_type, discount_value, min_order_value, max_discount) VALUES
    ('SAVE5',   '5% off orders of ₹2,000 or more, up to ₹500', 'PERCENTAGE', 5,   2000, 500),
    ('FLAT100', '₹100 off orders of ₹1,500 or more',           'FIXED',      100, 1500, NULL);
