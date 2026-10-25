-- Development sample catalog. Loaded only by the dev profile (see application-dev.yml).
-- Products have no images; upload them through the admin API.

INSERT INTO categories (name, slug, sort_order) VALUES
    ('Colas',            'colas',            1),
    ('Juices',           'juices',           2),
    ('Energy Drinks',    'energy-drinks',    3),
    ('Packaged Water',   'packaged-water',   4),
    ('Sparkling / Lime', 'sparkling-lime',   5);

INSERT INTO products (category_id, name, slug, brand, flavor, unit_volume_ml, units_per_case, price,
                      original_price, cost_price, stock_quantity, review_count, badge, description, created_at)
SELECT c.id, p.name, p.slug, p.brand, p.flavor, p.ml, p.units, p.price, p.original, p.cost, p.stock,
       p.reviews, p.badge, p.description, now() - make_interval(days => p.age_days)
FROM (VALUES
    ('colas',          'Thums Up 750ml PET',        'thums-up-750ml-pet',        'Thums Up',  'Cola',        750,  12,  420.00,  480.00,  370.00, 80, 120, 'Bestseller', 'Strong fizzy cola in 750ml PET bottles.', 1),
    ('colas',          'Diet Coke 330ml Can',       'diet-coke-330ml-can',       'Coca-Cola', 'Cola',        330,  24,  720.00,  780.00,  640.00, 40,  45, NULL,         'Zero-sugar cola in 330ml cans.',          2),
    ('colas',          'Coca-Cola 300ml Bottle',    'coca-cola-300ml-bottle',    'Coca-Cola', 'Cola',        300,  24,  480.00,  NULL,    420.00, 60,  98, NULL,         'Classic cola, returnable-size bottle.',   20),
    ('juices',         'Real Orange Juice 1L',      'real-orange-juice-1l',      'Real',      'Orange',     1000,  12,  780.00,  840.00,  690.00, 30,  37, NULL,         'Orange fruit juice in 1L tetra packs.',   3),
    ('juices',         'Maaza Mango 600ml',         'maaza-mango-600ml',         'Maaza',     'Mango',       600,  24,  900.00,  960.00,  800.00, 50,  88, 'Popular',    'Mango drink in 600ml PET bottles.',       4),
    ('juices',         'Slice Aam Panna 250ml',     'slice-aam-panna-250ml',     'Slice',     'Raw mango',   250,  30,  450.00,  NULL,    395.00, 25,  14, 'New',        'Tangy raw-mango drink.',                  0),
    ('energy-drinks',  'Sting Energy 250ml',        'sting-energy-250ml',        'Sting',     'Berry',       250,  24,  480.00,  540.00,  410.00, 70,  76, NULL,         'Energy drink in 250ml PET bottles.',      5),
    ('energy-drinks',  'Monster Sugar Free 350ml',  'monster-sugar-free-350ml',  'Monster',   'Original',    350,  12, 1440.00, 1560.00, 1290.00, 15,  22, NULL,         'Sugar-free energy drink cans.',           6),
    ('energy-drinks',  'Red Bull Energy 250ml',     'red-bull-energy-250ml',     'Red Bull',  'Original',    250,  24, 2280.00,  NULL,   2050.00, 20,  64, 'Premium',    'Energy drink in 250ml cans.',             30),
    ('packaged-water', 'Bisleri 1L Packaged Water', 'bisleri-1l-packaged-water', 'Bisleri',   NULL,         1000,  12,  240.00,  NULL,    200.00, 150, 140, 'Bestseller', 'Packaged drinking water, 1L.',            40),
    ('packaged-water', 'Kinley 500ml Water',        'kinley-500ml-water',        'Kinley',    NULL,          500,  24,  240.00,  NULL,    205.00, 90,  52, NULL,         'Packaged drinking water, 500ml.',         35),
    ('packaged-water', 'Aquafina 200ml Mini',       'aquafina-200ml-mini',       'Aquafina',  NULL,          200,  30,  180.00,  NULL,    150.00, 0,   18, NULL,         'Mini water bottles for counters.',        25),
    ('sparkling-lime', 'Sprite 300ml Bottle',       'sprite-300ml-bottle',       'Sprite',    'Lemon-lime',  300,  24,  480.00,  NULL,    420.00, 55,  71, NULL,         'Clear lemon-lime soft drink.',            15),
    ('sparkling-lime', '7UP 750ml PET',             '7up-750ml-pet',             '7UP',       'Lemon-lime',  750,  12,  420.00,  NULL,    365.00, 45,  33, NULL,         'Lemon-lime soft drink, 750ml PET.',       12),
    ('sparkling-lime', 'Limca 250ml Bottle',        'limca-250ml-bottle',        'Limca',     'Lime',        250,  24,  360.00,  NULL,    310.00, 35,  29, NULL,         'Lime and lemon fizzy drink.',             10)
) AS p(category_slug, name, slug, brand, flavor, ml, units, price, original, cost, stock, reviews, badge, description, age_days)
JOIN categories c ON c.slug = p.category_slug;

-- Bulk pricing: 10% off from 10 cases, 15% off from 25 cases (rounded to the rupee).
INSERT INTO price_slabs (product_id, min_qty, max_qty, price_per_case)
SELECT id, 10, 24, round(price * 0.90) FROM products;
INSERT INTO price_slabs (product_id, min_qty, max_qty, price_per_case)
SELECT id, 25, NULL, round(price * 0.85) FROM products;

INSERT INTO banners (title, subtitle, cta_text, link, sort_order) VALUES
    ('Bulk case pricing',    'Order 10+ cases and the slab discount applies automatically.', 'Shop cases',  '/shop',                         0),
    ('Next-day delivery',    'Free delivery on your route. Prepaid, COD or credit.',          'Order now',   '/shop',                         1),
    ('Energy drinks in stock', 'Sting, Monster and Red Bull by the case.',                    'Explore',     '/shop?category=energy-drinks',  2);

UPDATE store_settings SET value = 'Telangana, India' WHERE key = 'store_address';
UPDATE store_settings SET value = '500' WHERE key = 'delivery_charge';
UPDATE store_settings SET value = '5000' WHERE key = 'free_delivery_above';
UPDATE store_settings SET value = '1000' WHERE key = 'min_order_value';
