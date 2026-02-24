# Catalog Domain - Entity Relationships

## Entity Relationship Diagram

```
┌───────────────┐       ┌───────────────┐
│   Product     │ 1   1 │  Inventory    │
│  (catalog)    ├───────┤ (inventory)   │
└───────┬───────┘       └───────────────┘
        │ 1
        │
        │ *
┌───────┴───────┐
│  OrderItem    │
│  (orders)     │
└───────────────┘
```

## Core Entities

### Product (`products` table)

> Defined in `Product.java`

The **aggregate root** of the catalog domain. Represents a purchasable item in the system.

| Field | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | `UUID` | PK, auto-generated | Unique identifier |
| `name` | `String` | NOT NULL | Display name |
| `description` | `String` | TEXT, nullable | Optional long description |
| `price` | `BigDecimal(12,2)` | NOT NULL | Current selling price |
| `sku` | `String(50)` | UNIQUE, NOT NULL | Stock Keeping Unit — business identifier |
| `active` | `boolean` | NOT NULL, default `true` | Soft-delete flag |
| `version` | `int` | NOT NULL, default `0` | Optimistic locking counter (`@Version`) |
| `createdAt` | `LocalDateTime` | NOT NULL, immutable | Auto-set on insert (`@CreationTimestamp`) |
| `updatedAt` | `LocalDateTime` | NOT NULL | Auto-set on update (`@UpdateTimestamp`) |

### Inventory (`inventory` table)

> Defined in `Inventory.java`

A **companion entity** that tracks stock quantity for a single product. Separated from Product to isolate high-contention stock writes from low-contention catalog reads.

| Field | Type | Constraints | Purpose |
|---|---|---|---|
| `id` | `UUID` | PK, auto-generated | Unique identifier |
| `product` | `Product` | FK, UNIQUE, NOT NULL | The product this record tracks |
| `quantity` | `int` | NOT NULL, default `0` | Available stock count |
| `version` | `int` | NOT NULL, default `0` | Optimistic locking counter (`@Version`) |
| `updatedAt` | `LocalDateTime` | NOT NULL | Auto-set on update (`@UpdateTimestamp`) |

## Relationships in Detail

### 1. Product ↔ Inventory (`@OneToOne`)

> Defined in `Inventory.java:33-35`

- **One Product has exactly one Inventory record** — every product tracks its stock in a dedicated row
- `FetchType.LAZY` — inventory is only loaded when explicitly accessed (avoids joining on every product query)
- `unique = true` on `@JoinColumn` — enforces the 1:1 cardinality at the database level
- **Unidirectional** from Inventory → Product (Product has no `@OneToOne Inventory` field) — this keeps the catalog module decoupled from the inventory module
- **Both entities use `@Version`** — concurrent stock updates or product edits return 409 Conflict instead of silently overwriting

### 2. Product ← OrderItem (`@ManyToOne`, cross-module)

> Defined in `OrderItem.java:33-35`

- **Many OrderItems can reference the same Product** — a product appears in multiple orders over time
- **Unidirectional** from OrderItem → Product (Product has no `List<OrderItem>` back-reference)
- OrderItem snapshots `unitPrice` and `subtotal` at order creation time — this means the catalog can freely update `Product.price` without corrupting historical order data
- Only `ACTIVE` products can be ordered; deactivated products remain in the DB for historical order integrity

## Product Lifecycle (State Machine)

```
                ┌──── Admin activates ────┐
                │                         │
  [Created] → ACTIVE ←───────────────────┘
                │
                └──── Admin deactivates ──→ INACTIVE
                                              │
                                              └──── Admin activates ──→ ACTIVE
```

- Products are created as `active = true` by default
- **Active products**: visible in public catalog, can be ordered
- **Inactive products**: hidden from public listings, cannot be ordered, but preserved for historical order references
- This is a **soft-delete** pattern — products are never physically deleted

## Inventory Operations

The inventory service exposes five operations, split by audience:

### Admin Operations (via `AdminInventoryController`)

| Operation | HTTP | Method | Description |
|---|---|---|---|
| **List all** | `GET /api/v1/admin/inventory` | `getAllInventory()` | Paginated list with product details (JOIN FETCH) |
| **View one** | `GET /api/v1/admin/inventory/{productId}` | `getInventoryByProductId()` | Stock for a specific product |
| **Set stock** | `PUT /api/v1/admin/inventory/{productId}` | `setStock()` | Absolute override (e.g., after physical count) |
| **Adjust stock** | `PATCH /api/v1/admin/inventory/{productId}/adjust` | `adjustStock()` | Relative change (+/-), rejects negative results |

### Internal Operations (called by OrderService, no HTTP endpoint)

| Operation | Method | Description |
|---|---|---|
| **Decrement** | `decrementStock(productId, qty)` | Reserves stock during order creation — throws `InsufficientStockException` if not enough |
| **Restore** | `restoreStock(productId, qty)` | Returns stock during order cancellation |

**Key detail:** `decrementStock()` and `restoreStock()` have **no `@Transactional` annotation** — they participate in the caller's transaction. This ensures atomicity: inventory decrement + order creation happen in a single transaction, so either both succeed or both roll back.

## Catalog API Endpoints

### Public (no auth required)

| HTTP | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/products` | Browse active products with filters (name, minPrice, maxPrice) + pagination |
| `GET` | `/api/v1/products/{id}` | Get a single product by ID |

### Admin (requires `ADMIN` role)

| HTTP | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/admin/products` | Create product (validates unique SKU) |
| `PUT` | `/api/v1/admin/products/{id}` | Update product (validates unique SKU excluding self) |
| `PATCH` | `/api/v1/admin/products/{id}/status` | Activate/deactivate product (soft delete) |

## Query Capabilities

The `ProductRepository.findActiveProducts()` JPQL query supports dynamic filtering:

```sql
SELECT p FROM Product p WHERE p.active = true
  AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
  AND (:minPrice IS NULL OR p.price >= :minPrice)
  AND (:maxPrice IS NULL OR p.price <= :maxPrice)
```

- All filter parameters are **optional** — passing `null` skips that filter
- Name search is **case-insensitive substring match**
- Price filters are **inclusive** (>=, <=)
- Only `active = true` products are returned (inactive products are invisible to customers)

## DTO Layer

```
┌─────────────────┐          ┌──────────────────┐          ┌──────────────────────┐
│ ProductRequest   │  ──→    │ Product (Entity)  │  ──→    │ ProductResponse       │
│ (name,desc,      │ create/ │ (full DB fields)  │  map    │ (id,name,desc,price,  │
│  price,sku)      │ update  │                   │         │  sku,active,createdAt) │
└─────────────────┘          └──────────────────┘          └──────────────────────┘

┌──────────────────────┐
│ ProductStatusRequest  │  ──→  Updates only `active` field
│ (active: Boolean)     │
└──────────────────────┘
```

- **ProductRequest**: used for both create (`POST`) and update (`PUT`) — validates `@NotBlank` name/sku, `@NotNull @Positive` price
- **ProductStatusRequest**: dedicated DTO for the `PATCH .../status` endpoint — only carries the `active` boolean
- **ProductResponse**: returned to clients — never exposes `version` or `updatedAt` (internal fields)
- Mapping is done manually in `ProductServiceImpl.mapToResponse()` (no MapStruct in this module)

## Key Design Decisions

| Pattern | Where | Why |
|---|---|---|
| **Separate Product/Inventory entities** | `catalog` + `inventory` modules | Isolates high-contention stock writes from catalog reads; different `@Version` counters |
| **Optimistic locking** | `Product.version`, `Inventory.version` | Concurrent admin edits or stock changes return 409 instead of silent overwrites |
| **Soft delete via `active` flag** | `Product.active` | Products referenced by historical orders can't be physically deleted |
| **SKU uniqueness** | `@Column(unique = true)` + service-layer validation | Dual enforcement: DB constraint as safety net + service check for friendly error messages |
| **Price snapshot in OrderItem** | `OrderItem.unitPrice` | Catalog price changes don't corrupt historical order data |
| **Unidirectional relationships** | Product has no back-refs | Keeps catalog module independent — inventory and orders depend on catalog, not the reverse |
| **No `@Transactional` on decrement/restore** | `InventoryServiceImpl` | Participates in caller's transaction for atomicity with order creation/cancellation |
| **JOIN FETCH on inventory listing** | `InventoryRepository.findAllWithProduct()` | Avoids N+1 queries when listing inventory with product names |

## Cross-Module Boundaries

```
catalog (Product)
    ↑                    ↑
    │ @OneToOne           │ @ManyToOne
    │                     │
inventory (Inventory)   orders (OrderItem)
    ↑
    │ decrementStock() / restoreStock()
    │
  orders (OrderService)
```

- **catalog** is a dependency of both **inventory** and **orders** — it has no knowledge of either
- **inventory** is called by **orders** via direct service injection (`InventoryService.decrementStock()`)
- The module boundary is enforced by package structure, not runtime isolation — all modules run in the same JVM
