-- S'Mart Retail initial schema.
-- Money is NUMERIC(12,2) in rupees; all timestamps are TIMESTAMPTZ.
-- Enumerations are TEXT with CHECK constraints so they map cleanly to JPA enums.

-- ---------------------------------------------------------------------------
-- Stores and delivery routes
-- ---------------------------------------------------------------------------
CREATE TABLE stores (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    gstin       TEXT,
    phone       TEXT,
    email       TEXT,
    address     TEXT,
    city        TEXT,
    pincode     TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE delivery_routes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             TEXT NOT NULL,
    area             TEXT,
    days             TEXT[] NOT NULL DEFAULT '{}',          -- e.g. {MON,THU}
    store_id         UUID REFERENCES stores (id) ON DELETE SET NULL,
    color            TEXT,
    zone_type        TEXT CHECK (zone_type IN ('POLYGON', 'CIRCLE')),
    zone_polygon     JSONB,                                  -- [{"lat":..,"lng":..}, ...]
    zone_center_lat  NUMERIC(9, 6),
    zone_center_lng  NUMERIC(9, 6),
    zone_radius_km   NUMERIC(8, 3),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order       INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Users, roles, auth
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          TEXT UNIQUE,                 -- stored lower-cased
    phone          TEXT UNIQUE,                 -- stored as 10 digits
    password_hash  TEXT NOT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_email_or_phone CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE user_roles (
    user_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role     TEXT NOT NULL CHECK (role IN ('ADMIN', 'CASHIER', 'CUSTOMER')),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,           -- SHA-256 of the opaque token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE profiles (
    user_id                UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    full_name              TEXT,
    email                  TEXT,
    phone                  TEXT,
    shop_name              TEXT,
    owner_name             TEXT,
    gst_number             TEXT,
    shop_address           TEXT,
    shop_city              TEXT,
    shop_state             TEXT,
    shop_pincode           TEXT,
    shop_lat               NUMERIC(9, 6),
    shop_lng               NUMERIC(9, 6),
    shop_place_id          TEXT,
    shop_profile_complete  BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_route_id      UUID REFERENCES delivery_routes (id) ON DELETE SET NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE addresses (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    label          TEXT NOT NULL DEFAULT 'Home',
    full_name      TEXT NOT NULL,
    phone          TEXT NOT NULL,
    address_line1  TEXT NOT NULL,
    address_line2  TEXT,
    city           TEXT NOT NULL DEFAULT '',
    state          TEXT NOT NULL DEFAULT 'Telangana',
    pincode        TEXT NOT NULL,
    is_default     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_addresses_user ON addresses (user_id);

-- ---------------------------------------------------------------------------
-- Shops (kirana buyers) and credit ledger
-- ---------------------------------------------------------------------------
CREATE TABLE shops (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id      UUID REFERENCES users (id) ON DELETE SET NULL,
    name               TEXT NOT NULL,
    gstin              TEXT,
    phone              TEXT,
    address_line1      TEXT,
    address_line2      TEXT,
    city               TEXT,
    pincode            TEXT,
    delivery_route_id  UUID REFERENCES delivery_routes (id) ON DELETE SET NULL,
    credit_limit       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_shops_owner ON shops (owner_user_id);

CREATE TABLE shop_payments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id      UUID NOT NULL REFERENCES shops (id) ON DELETE CASCADE,
    amount       NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    method       TEXT NOT NULL CHECK (method IN ('CASH', 'UPI', 'BANK', 'CHEQUE')),
    reference    TEXT,
    received_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_shop_payments_shop ON shop_payments (shop_id);

-- ---------------------------------------------------------------------------
-- Catalog
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL UNIQUE,
    slug        TEXT NOT NULL UNIQUE,
    image_url   TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id          UUID REFERENCES categories (id) ON DELETE SET NULL,
    name                 TEXT NOT NULL,
    slug                 TEXT NOT NULL UNIQUE,
    brand                TEXT,
    flavor               TEXT,
    unit_volume_ml       INT,
    units_per_case       INT NOT NULL DEFAULT 1 CHECK (units_per_case > 0),
    weight               TEXT,
    unit                 TEXT NOT NULL DEFAULT 'case',
    price                NUMERIC(12, 2) NOT NULL CHECK (price >= 0),     -- per case
    original_price       NUMERIC(12, 2),                                 -- strike-through MRP
    cost_price           NUMERIC(12, 2),
    stock_quantity       INT NOT NULL DEFAULT 0,                          -- in cases
    low_stock_threshold  INT NOT NULL DEFAULT 5,
    barcode              TEXT,
    badge                TEXT,
    description          TEXT,
    ingredients          TEXT,
    nutrition            JSONB,
    rating               NUMERIC(2, 1) NOT NULL DEFAULT 0,
    review_count         INT NOT NULL DEFAULT 0,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_active ON products (is_active);

CREATE TABLE product_images (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order  INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_product_images_product ON product_images (product_id);

-- Bulk pricing: the slab with the highest min_qty <= ordered cases applies.
CREATE TABLE price_slabs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    min_qty         INT NOT NULL CHECK (min_qty > 0),
    max_qty         INT CHECK (max_qty IS NULL OR max_qty >= min_qty),
    price_per_case  NUMERIC(12, 2) NOT NULL CHECK (price_per_case >= 0),
    UNIQUE (product_id, min_qty)
);

CREATE TABLE wishlists (
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, product_id)
);

CREATE TABLE coupons (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code             TEXT NOT NULL UNIQUE,         -- stored upper-cased
    description      TEXT,
    discount_type    TEXT NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    discount_value   NUMERIC(12, 2) NOT NULL CHECK (discount_value > 0),
    min_order_value  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    max_discount     NUMERIC(12, 2),
    usage_limit      INT,
    used_count       INT NOT NULL DEFAULT 0,
    valid_from       TIMESTAMPTZ,
    valid_until      TIMESTAMPTZ,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Orders
-- ---------------------------------------------------------------------------
CREATE TABLE couriers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    phone       TEXT,
    vehicle     TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE SEQUENCE order_number_seq START 1001;

CREATE TABLE orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number        TEXT NOT NULL UNIQUE,
    user_id             UUID REFERENCES users (id) ON DELETE SET NULL,
    shop_id             UUID REFERENCES shops (id) ON DELETE SET NULL,
    store_id            UUID REFERENCES stores (id) ON DELETE SET NULL,
    channel             TEXT NOT NULL CHECK (channel IN ('ONLINE', 'POS')),
    status              TEXT NOT NULL DEFAULT 'PLACED' CHECK (status IN (
                            'PLACED', 'CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED',
                            'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED')),
    subtotal            NUMERIC(12, 2) NOT NULL,
    shipping_charge     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total               NUMERIC(12, 2) NOT NULL,
    total_cases         INT NOT NULL DEFAULT 0,
    coupon_code         TEXT,
    payment_method      TEXT NOT NULL CHECK (payment_method IN ('UPI', 'COD', 'CREDIT', 'CASH')),
    payment_status      TEXT NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN (
                            'PENDING', 'PENDING_VERIFICATION', 'PAID', 'FAILED', 'REFUNDED')),
    payment_terms       TEXT NOT NULL DEFAULT 'PREPAID' CHECK (payment_terms IN ('PREPAID', 'CREDIT')),
    payment_screenshot  TEXT,
    shipping_address    JSONB,
    shop_name           TEXT,
    customer_name       TEXT,
    customer_phone      TEXT,
    delivery_route_id   UUID REFERENCES delivery_routes (id) ON DELETE SET NULL,
    delivery_date       DATE,
    cashier_id          UUID REFERENCES users (id) ON DELETE SET NULL,
    courier_id          UUID REFERENCES couriers (id) ON DELETE SET NULL,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_shop ON orders (shop_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_route_date ON orders (delivery_route_id, delivery_date);
CREATE INDEX idx_orders_created ON orders (created_at);

CREATE TABLE order_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    UUID REFERENCES products (id) ON DELETE SET NULL,
    product_name  TEXT NOT NULL,                  -- snapshot at order time
    quantity      INT NOT NULL CHECK (quantity > 0),
    price         NUMERIC(12, 2) NOT NULL,         -- per case, after slab
    cost_price    NUMERIC(12, 2),                  -- snapshot for profit reports
    total         NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

-- ---------------------------------------------------------------------------
-- Inventory and purchasing
-- ---------------------------------------------------------------------------
CREATE TABLE suppliers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    phone       TEXT,
    address     TEXT,
    notes       TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stock_receipts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id     UUID REFERENCES suppliers (id) ON DELETE SET NULL,
    store_id        UUID REFERENCES stores (id) ON DELETE SET NULL,
    receipt_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    invoice_number  TEXT,
    notes           TEXT,
    total_cost      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_by      UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stock_receipt_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id  UUID NOT NULL REFERENCES stock_receipts (id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products (id),
    quantity    INT NOT NULL CHECK (quantity > 0),
    cost_price  NUMERIC(12, 2) NOT NULL,
    total       NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_stock_receipt_items_receipt ON stock_receipt_items (receipt_id);

CREATE TABLE stock_adjustments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    delta       INT NOT NULL CHECK (delta <> 0),
    reason      TEXT NOT NULL CHECK (reason IN ('DAMAGE', 'EXPIRED', 'CORRECTION', 'OTHER')),
    notes       TEXT,
    created_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_stock_adjustments_product ON stock_adjustments (product_id);

CREATE TABLE expenses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         TEXT NOT NULL,
    category      TEXT NOT NULL DEFAULT 'general',
    amount        NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    description   TEXT,
    expense_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by    UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_expenses_date ON expenses (expense_date);

-- ---------------------------------------------------------------------------
-- Storefront content, notifications, settings
-- ---------------------------------------------------------------------------
CREATE TABLE banners (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT NOT NULL,
    subtitle    TEXT,
    cta_text    TEXT,
    link        TEXT,
    image_url   TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE push_notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT NOT NULL,
    body        TEXT NOT NULL,
    type        TEXT NOT NULL DEFAULT 'PROMO',
    target      TEXT NOT NULL DEFAULT 'ALL',
    created_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       TEXT NOT NULL UNIQUE,              -- FCM registration token
    platform    TEXT NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE store_settings (
    key         TEXT PRIMARY KEY,
    value       TEXT,
    is_public   BOOLEAN NOT NULL DEFAULT FALSE,    -- exposed via /api/settings/public
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO store_settings (key, value, is_public) VALUES
    ('store_name',          'S''Mart Retail', TRUE),
    ('store_phone',         '',               TRUE),
    ('store_email',         '',               TRUE),
    ('store_address',       '',               TRUE),
    ('whatsapp_number',     '',               TRUE),
    ('store_open',          'true',           TRUE),
    ('delivery_charge',     '0',              TRUE),
    ('free_delivery_above', '0',              TRUE),
    ('min_order_value',     '0',              TRUE),
    ('upi_id',              '',               TRUE),
    ('gst_number',          '',               TRUE),
    ('fssai_number',        '',               TRUE),
    ('show_certifications', 'true',           TRUE);
