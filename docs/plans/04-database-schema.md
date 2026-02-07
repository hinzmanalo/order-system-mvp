# Feature 04: Database Schema (Flyway Migrations)

**Priority**: Foundation
**Dependencies**: 01-backend-scaffolding, 02-docker-infrastructure
**Parallel with**: None (sequential gate)
**Blocks**: 05-common-module

---

## Overview

Create all database tables via versioned Flyway migration scripts. This includes the full schema for users, products, inventory, orders, order items, payments, and refresh tokens, plus seed data for development.

## User Stories

- All stories depend on the database schema existing

## Tasks

### 4.1 Migration directory

- [ ] Create `backend/src/main/resources/db/migration/`

### 4.2 V1 — Users table

- [ ] Create `V1__create_users_table.sql`:
  ```sql
  CREATE TABLE users (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      email VARCHAR(255) NOT NULL UNIQUE,
      password_hash VARCHAR(255) NOT NULL,
      first_name VARCHAR(100) NOT NULL,
      last_name VARCHAR(100) NOT NULL,
      role VARCHAR(20) NOT NULL DEFAULT 'USER',
      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMP NOT NULL DEFAULT NOW()
  );
  ```

### 4.3 V2 — Products table

- [ ] Create `V2__create_products_table.sql`:
  - `id` UUID PK with `gen_random_uuid()`
  - `name` VARCHAR(255) NOT NULL
  - `description` TEXT (nullable)
  - `price` DECIMAL(12,2) NOT NULL, CHECK (price > 0)
  - `sku` VARCHAR(50) NOT NULL UNIQUE
  - `active` BOOLEAN NOT NULL DEFAULT TRUE
  - `version` INTEGER NOT NULL DEFAULT 0
  - `created_at`, `updated_at` TIMESTAMP NOT NULL DEFAULT NOW()

### 4.4 V3 — Inventory table

- [ ] Create `V3__create_inventory_table.sql`:
  - `id` UUID PK
  - `product_id` UUID NOT NULL, FK → products(id), UNIQUE
  - `quantity` INTEGER NOT NULL DEFAULT 0, CHECK (quantity >= 0)
  - `version` INTEGER NOT NULL DEFAULT 0
  - `updated_at` TIMESTAMP NOT NULL DEFAULT NOW()

### 4.5 V4 — Orders and order items tables

- [ ] Create `V4__create_orders_tables.sql`:
  - **orders** table:
    - `id` UUID PK
    - `user_id` UUID NOT NULL, FK → users(id)
    - `status` VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
    - `total_amount` DECIMAL(12,2) NOT NULL
    - `created_at`, `updated_at` TIMESTAMP NOT NULL DEFAULT NOW()
    - Indexes: `idx_orders_user_id`, `idx_orders_status`, `idx_orders_created_at`
  - **order_items** table:
    - `id` UUID PK
    - `order_id` UUID NOT NULL, FK → orders(id) ON DELETE CASCADE
    - `product_id` UUID NOT NULL, FK → products(id)
    - `quantity` INTEGER NOT NULL, CHECK (quantity > 0)
    - `unit_price` DECIMAL(12,2) NOT NULL
    - `subtotal` DECIMAL(12,2) NOT NULL
    - Index: `idx_order_items_order_id`

### 4.6 V5 — Payments table

- [ ] Create `V5__create_payments_table.sql`:
  - `id` UUID PK
  - `order_id` UUID NOT NULL, FK → orders(id)
  - `amount` DECIMAL(12,2) NOT NULL
  - `status` VARCHAR(20) NOT NULL
  - `idempotency_key` VARCHAR(255) NOT NULL UNIQUE
  - `gateway_reference` VARCHAR(255) (nullable)
  - `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
  - Indexes: `idx_payments_order_id`, `idx_payments_idempotency_key` (unique)

### 4.7 V6 — Refresh tokens table

- [ ] Create `V6__create_refresh_tokens_table.sql`:
  - `id` UUID PK
  - `user_id` UUID NOT NULL, FK → users(id) ON DELETE CASCADE
  - `token` VARCHAR(512) NOT NULL UNIQUE
  - `expires_at` TIMESTAMP NOT NULL
  - `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
  - Index: `idx_refresh_tokens_token` (unique)

### 4.8 V7 — Seed data

- [ ] Create `V7__seed_dev_data.sql`:
  - Admin user: `admin@orderhub.com` / `admin123` (BCrypt hash: `$2a$10$...`), role=ADMIN
  - Test user: `user@orderhub.com` / `user123` (BCrypt hash: `$2a$10$...`), role=USER
  - 5 products:
    - Smartphone X (SKU: PHONE-001, $299.99)
    - Laptop Pro (SKU: LAPTOP-001, $999.99)
    - Wireless Earbuds (SKU: AUDIO-001, $79.99)
    - USB-C Cable (SKU: CABLE-001, $12.99)
    - Phone Case (SKU: CASE-001, $19.99)
  - 5 inventory records: each with 50-100 units

## Verification

- [ ] Start PostgreSQL: `docker compose up db -d`
- [ ] Start backend: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- [ ] All 7 migrations apply successfully (check logs for "Successfully applied 7 migrations")
- [ ] Verify tables exist: connect to DB and list tables
- [ ] Verify seed data: query users (2 rows), products (5 rows), inventory (5 rows)

## Files Created

```
backend/src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_products_table.sql
├── V3__create_inventory_table.sql
├── V4__create_orders_tables.sql
├── V5__create_payments_table.sql
├── V6__create_refresh_tokens_table.sql
└── V7__seed_dev_data.sql
```
