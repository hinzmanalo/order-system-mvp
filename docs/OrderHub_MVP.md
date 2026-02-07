# OrderHub – Monolith Ordering System (MVP)

## Overview

OrderHub is a **portfolio-grade monolithic ordering system** built with a **modular monolith architecture**.
It demonstrates real-world backend engineering skills including transactional integrity, inventory control,
payment processing, and clean domain separation.

**Tech Stack**

- Java 17
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker & Docker Compose
- OpenAPI / Swagger
- (Optional) Spring Security

---

## Architecture Style

**Modular Monolith**

- Single deployable application
- Clear domain boundaries
- Clean separation of concerns
- Easy to evolve into microservices later

### Modules

- `catalog` – Product management
- `inventory` – Stock and reservation handling
- `orders` – Order lifecycle and workflow
- `payments` – Payment processing (gateway abstraction)
- `common` – Shared DTOs, errors, utilities

---

## Functional Scope (MVP)

### Customer

- Browse products
- Place an order
- Pay for an order
- View order status

### Admin

- Create/update products
- Adjust inventory stock
- Manage users with admin and user role. Admin can access Customer and Admin module. User can only access Admin

---

## Order Lifecycle

```
CREATED → CONFIRMED → PAID
          ↓
      CANCELLED
```

---

## Business Rules

1. Order placement is **atomic**
2. No overselling (optimistic locking)
3. Payment amount must equal order total
4. Only CONFIRMED orders can be paid
5. Idempotency support using `Idempotency-Key`
6. Cancellation releases inventory

---

## MVP Definition of Done

- Create product and stock
- Place order successfully
- Prevent order when stock insufficient
- Pay for confirmed order
- Handle failed payment
- Cancel order releases inventory
- APIs documented via Swagger

---

## Portfolio Value

- Clean modular monolith design
- Transaction management
- Inventory consistency
- Payment integration pattern
- Enterprise-ready backend architecture
