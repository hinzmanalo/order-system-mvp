# Orders Domain - Entity Relationships

## Entity Relationship Diagram

```
┌─────────┐       ┌─────────────┐       ┌───────────┐       ┌───────────┐
│  User    │ 1   * │   Order     │ 1   * │ OrderItem │ *   1 │  Product  │
│ (auth)   ├───────┤ (orders)    ├───────┤ (orders)  ├───────┤ (catalog) │
└─────────┘       └──────┬──────┘       └───────────┘       └───────────┘
                         │ 1
                         │
                         │ *
                  ┌──────┴──────┐
                  │  Payment    │
                  │ (payments)  │
                  └─────────────┘
```

## Relationships in Detail

### 1. User → Order (`@ManyToOne`)

> Defined in `Order.java:36-38`

- **One User has many Orders** — a customer can place multiple orders over time
- `FetchType.LAZY` — the User is only loaded from DB when `order.getUser()` is actually called (performance optimization)
- `nullable = false` — every order **must** belong to a user (no anonymous orders)
- The relationship is **unidirectional** from Order → User (User entity has no `List<Order>` field)

### 2. Order → OrderItem (`@OneToMany`)

> Defined in `Order.java:47-48`

- **One Order has many OrderItems** — like a shopping receipt with line items
- This is a **bidirectional** relationship:
  - Order side: `@OneToMany(mappedBy = "order")` — Order is the **inverse/non-owning** side
  - OrderItem side: `@ManyToOne` with `@JoinColumn(name = "order_id")` — OrderItem is the **owning** side (it holds the FK column)
- `cascade = CascadeType.ALL` — when you save/delete an Order, all its OrderItems are automatically saved/deleted too
- `orphanRemoval = true` — if you remove an OrderItem from the list, it gets deleted from the DB (not just unlinked)
- Helper methods `addItem()` / `removeItem()` on `Order.java:81-95` maintain **both sides** of the bidirectional relationship — this is critical because JPA only reads the owning side

### 3. OrderItem → Product (`@ManyToOne`)

> Defined in `OrderItem.java:33-35`

- **Many OrderItems can reference the same Product** — the same product can appear in different orders
- `FetchType.LAZY` — Product details loaded on demand
- **Price snapshot pattern**: OrderItem stores its own `unitPrice` and `subtotal` (`OrderItem.java:41-45`) — this is a deliberate design choice. Even if `Product.price` changes later, the historical order data stays accurate
- **Unidirectional** from OrderItem → Product (Product has no back-reference to orders)

### 4. Order → Payment (`@ManyToOne`)

> Defined in `Payment.java:33-35`

- **One Order can have many Payments** — this allows for retries. If a payment fails, the customer can try again with a new idempotency key
- **Unidirectional** from Payment → Order (Order has no `List<Payment>` field)
- `idempotencyKey` (`Payment.java:44`) is `unique = true` — prevents duplicate payment processing (e.g., user double-clicks "Pay")

## Order Lifecycle (State Machine)

```
                    ┌──── Payment SUCCESS ────→ PAID (terminal)
                    │
  [Created] → CONFIRMED
                    │
                    └──── Cancel ──────────→ CANCELLED (terminal)
```

- Orders start as `CONFIRMED` (inventory already reserved at creation time)
- `CONFIRMED → PAID`: when a Payment with `SUCCESS` status is recorded
- `CONFIRMED → CANCELLED`: inventory is **restored** atomically
- Both `PAID` and `CANCELLED` are **terminal states** — no further transitions allowed

## Key Design Decisions

| Pattern | Where | Why |
|---|---|---|
| **UUID PKs** | All entities | Avoids sequential ID exposure, safe for distributed systems |
| **Lazy loading** | All `@ManyToOne` | Prevents N+1 queries; only fetches related entities when accessed |
| **Price snapshot** | `OrderItem.unitPrice` | Historical accuracy — orders reflect prices at purchase time |
| **Optimistic locking** | `Product` (`@Version`) | Prevents concurrent updates from silently overwriting each other (returns 409) |
| **Cascade ALL + orphanRemoval** | Order → OrderItem | Order is the aggregate root; items have no independent lifecycle |
| **Unidirectional where possible** | User←Order, Product←OrderItem | Simpler model; avoids unnecessary coupling between modules |

## Cross-Module Boundaries

This is a **modular monolith** — the entities span 4 different packages:

- `auth` → `orders` → `payments` follows the dependency chain
- `catalog` is referenced by `orders` (OrderItem → Product)
- These are direct JPA references (same JVM), not REST calls — but the package boundaries keep the modules logically separated