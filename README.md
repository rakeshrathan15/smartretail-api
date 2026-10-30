# S'Mart Retail Backend

Spring Boot 4.1 · Java 21 · Gradle 9.8 · PostgreSQL 18 · Flyway · JWT (HS256)

## Run locally

Prerequisites: Java 21, a running PostgreSQL with the dev database:

```sql
CREATE ROLE smart_retail LOGIN PASSWORD 'smart_retail';
CREATE DATABASE smart_retail OWNER smart_retail;
```

```bash
./gradlew bootRun     # http://localhost:8080, dev profile
./gradlew test        # needs Docker (Testcontainers starts postgres:18-alpine)
```

Flyway creates the schema on startup (`src/main/resources/db/migration`).
The dev profile creates an admin from `application-dev.yml` (`admin@smartretail.local`).

## Configuration

| Env var | Purpose | Dev default |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Database | local `smart_retail` |
| `JWT_SECRET` | Base64 HMAC key, ≥ 32 bytes (`openssl rand -base64 48`) | dev-only key |
| `CORS_ALLOWED_ORIGINS` | Comma-separated web origins | localhost ports |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | First admin, created if no admin exists | dev admin |
| `SPRING_PROFILES_ACTIVE` | Use `prod` (anything but `dev`) outside your machine | `dev` |

Outside dev, `JWT_SECRET` is required and the app refuses to start without it.

## Auth API

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | `fullName, email?, phone?, password` | Email or mobile required; role CUSTOMER |
| POST | `/api/auth/login` | `identifier, password` | Identifier is an email or mobile number |
| POST | `/api/auth/refresh` | `refreshToken` | Rotates the token; reusing an old token signs out all sessions |
| POST | `/api/auth/logout` | `refreshToken` | Revokes the token |
| GET | `/api/me` | | Needs `Authorization: Bearer <accessToken>` |

Access tokens last 15 minutes and carry `sub` (user id) and `roles`. Refresh tokens last 30 days.
Mobile numbers are stored as 10 digits, so `+91 98765 43210`, `09876543210` and `9876543210` are the same account.

Route rules (`security/SecurityConfig.java`): `/api/admin/**` needs ADMIN, `/api/pos/**` needs CASHIER or ADMIN,
`GET` on `/api/products|categories|banners/**` and `/api/settings/public` is public, everything else needs a login.

Errors are returned as RFC 9457 problem details; validation errors include an `errors` map by field.

## Catalog API

Try every request from IntelliJ with [`http/catalog.http`](http/catalog.http).

| Method | Path | Notes |
|---|---|---|
| GET | `/api/categories` | Active categories, in sort order |
| GET | `/api/products` | `category` (slug), `q`, `deals=true`, `sort` = POPULAR · PRICE_ASC · PRICE_DESC · NEWEST · NAME, `page`, `size` (max 50) |
| GET | `/api/products/{slug}` | Detail with images and bulk price slabs |
| GET | `/api/banners` · `/api/settings/public` | Home carousel and store info |
| GET/POST/PUT/DELETE | `/api/admin/categories[/{id}]` | Delete refused while products use it |
| GET/POST/PUT/DELETE | `/api/admin/products[/{id}]` | Admin view adds cost price, stock and inactive items |
| PUT | `/api/admin/products/{id}/slabs` | Replaces all slabs; rejects overlaps and prices above base |
| POST · PUT · DELETE | `/api/admin/products/{id}/images[/{imageId}[/primary]]` | Multipart `file`; JPG/PNG/WebP up to 5 MB |
| GET/POST/PUT/DELETE | `/api/admin/banners[/{id}]`, `POST /{id}/image`, `PUT /order` | |

Bulk pricing: an order of N cases pays the slab with the highest `minQty ≤ N` (`Product.pricePerCaseFor`).
Uploads are saved under `uploads/` (`STORAGE_DIR`) and served at `/uploads/**`; swap `LocalFileStorage` for S3 in production.
The dev profile also loads `db/seed` (15 sample products, 5 categories, 3 banners).

## Cart, checkout and orders

Try them from IntelliJ with [`http/checkout.http`](http/checkout.http). The app keeps the cart on the phone
and sends only product ids and case counts; **every price is worked out on the server** (`CheckoutPricing`).

| Method | Path | Notes |
|---|---|---|
| POST | `/api/checkout/quote` | Prices a cart; returns lines, coupon result, delivery, total, credit and blocking problems. Changes nothing |
| POST | `/api/orders` | Places an order; refused (409) if anything changed since the quote (send `expectedTotal`) |
| POST | `/api/me/payment-screenshots` | Upload for UPI orders; returns the url to send with the order |
| GET | `/api/me/orders`, `/api/me/orders/{id}` | Own orders only |
| GET/POST/PUT/DELETE | `/api/me/addresses[/{id}]` | `makeDefault`; first address is the default |
| GET/POST/PUT/DELETE | `/api/admin/coupons[/{id}]` | Used coupons can be deactivated, not deleted |
| GET · PUT | `/api/admin/shops`, `/api/admin/shops/credit` | Credit limit per shop owner; 0 turns credit off |

Pricing rules, in order:
1. Each product pays its bulk slab price for its case count.
2. A coupon discounts the subtotal: percentage capped at `max_discount`, or a fixed amount; checks minimum order, dates and usage limit.
3. Delivery is free when subtotal − discount reaches `free_delivery_above`, otherwise `delivery_charge`.
4. The subtotal must reach `min_order_value`, the store must be open, and every line must be in stock.

Placing an order locks the product rows (in id order, so overlapping carts can't deadlock), re-prices,
takes the stock, redeems the coupon atomically and, for credit, locks the shop and checks
`outstanding + total ≤ credit_limit`. Payment: COD → `PENDING`; UPI → `PENDING_VERIFICATION` until an admin
checks the screenshot; CREDIT → `payment_terms = CREDIT`. Order numbers are `SR` + a database sequence.

## Settings and shop profile

| Method | Path | Notes |
|---|---|---|
| GET · PUT | `/api/admin/settings` | Admin only. Store open/closed, delivery charge, free delivery above, minimum order, UPI ID, store contact details, GSTIN, FSSAI, show certifications. Checkout uses the new values at once |
| GET | `/api/settings/public` | Public settings for the app and website (store open, charges, contact, certifications) |
| GET · PUT | `/api/me/shop-profile` | The shop owner's shop name, owner, mobile, GSTIN, address and map pin (lat/lng must be in India). Saving marks the profile complete and adds a default "Shop" delivery address if there is none |

## Stores, delivery routes, customers and pick-lists

| Method | Path | Notes |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/admin/stores[/{id}]` | Warehouses and branches. A store with routes cannot be deleted. `PATCH .../active` turns one on or off |
| GET/POST/PUT/DELETE | `/api/admin/routes[/{id}]` | Name, days, store, colour, priority and area (polygon or circle). A route with orders cannot be deleted; turn it off. `PATCH .../active` |
| GET | `/api/admin/routes/shop-pins` | Every shop's map pin and route, for the routes map |
| POST | `/api/admin/routes/reassign` | Places every shop again and reports how many landed on a route |
| GET | `/api/admin/customers` | Paged. `q` searches; `route` is `all`, `unassigned` or a route id |
| GET | `/api/admin/pick-list?route=&date=` | What to load for a route (or `unassigned`) and delivery date |
| GET | `/api/me/delivery-route` | The signed-in shop's route and next delivery day |

How it fits together:
- **Placing shops** (`RouteAssignmentService`): the first active route, by priority then name, whose area contains the shop's pin. It runs when a shop saves its location (`ShopLocationChanged` event, so the profile code does not depend on routes) and after every route change. Plain maths in Java (`DeliveryZone`), so PostGIS is not needed.
- **Delivery dates** (`DeliveryDays`, `DeliverySchedule`): an order is stamped with the route's next delivery day after today, or the next day when the shop is on no route. Orders keep the route and date they were placed with, even if the shop later moves.
- **Pick-lists** leave out cancelled and returned orders.

## Layout

```
com.smartretail
├── auth       register/login/refresh/logout, tokens, bootstrap admin
├── user       User, Role, Profile
├── security   Spring Security, JWT encoder/decoder, CORS
└── common     ApiException and error handler
```

The full schema (catalog, orders, routes, credit ledger, inventory, POS, settings) is already in
`V1__init_schema.sql`; feature packages for those come next.
