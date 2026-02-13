# Feature 09: Orders Backend

**Priority**: Core
**Dependencies**: 06-auth-backend, 07-catalog-backend, 08-inventory-backend
**Parallel with**: None
**Blocks**: 10-payments-backend, 14-frontend-cart-orders-payments

---

## Overview

Implement the order management module — the most complex backend feature. Includes atomic order creation with inventory decrement, order cancellation with inventory restore, order status lifecycle (CONFIRMED → PAID / CANCELLED), and both customer and admin endpoints.

## User Stories

- US-009: Place order (checkout)
- US-010: View order history
- US-011: View order detail
- US-012: Cancel order
- US-019: Admin view all orders
- US-020: Admin cancel any order
- US-024: Concurrent order handling

## Tasks

### 9.1 Entities

- [x] `com.orderhub.orders.entity.OrderStatus.java` — enum: `CONFIRMED`, `PAID`, `CANCELLED`
- [x] `com.orderhub.orders.entity.Order.java`:
  - `@Table(name = "orders")`
  - `id` UUID
  - `user` User, `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "user_id")`
  - `status` OrderStatus, `@Enumerated(EnumType.STRING)`
  - `totalAmount` BigDecimal
  - `items` List\<OrderItem\>, `@OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)`
  - `createdAt`, `updatedAt` LocalDateTime
- [x] `com.orderhub.orders.entity.OrderItem.java`:
  - `@Table(name = "order_items")`
  - `id` UUID
  - `order` Order, `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "order_id")`
  - `product` Product, `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "product_id")`
  - `quantity` int
  - `unitPrice` BigDecimal
  - `subtotal` BigDecimal

### 9.2 Repository

- [x] `com.orderhub.orders.repository.OrderRepository.java`:
  - `Page<Order> findByUserId(UUID userId, Pageable pageable)`
  - `Optional<Order> findByIdAndUserId(UUID id, UUID userId)`
  - Custom `@Query` for filtered search:
    - Filter by userId (optional, for customer vs admin)
    - Filter by status (optional)
    - Filter by date range: `createdAt >= :after AND createdAt <= :before` (optional)
    - Paginated

### 9.3 DTOs

- [x] `OrderItemRequest.java` — `@NotNull` productId (UUID), `@NotNull @Min(1)` quantity (Integer)
- [x] `CreateOrderRequest.java` — `@NotEmpty @Valid` List\<OrderItemRequest\> items
- [x] `OrderResponse.java` — id, userId, status, items (List\<OrderItemResponse\>), totalAmount, createdAt
- [x] `OrderItemResponse.java` — productId, productName, quantity, unitPrice, subtotal

### 9.4 Service

- [x] `com.orderhub.orders.service.OrderService.java` — interface:
  - `createOrder(UUID userId, CreateOrderRequest)` → OrderResponse
  - `getOrderById(UUID orderId, UUID userId)` → OrderResponse
  - `getUserOrders(UUID userId, String status, LocalDateTime after, LocalDateTime before, Pageable)` → Page\<OrderResponse\>
  - `cancelOrder(UUID orderId, UUID userId)` → OrderResponse
  - `getAllOrders(String status, LocalDateTime after, LocalDateTime before, Pageable)` → Page\<OrderResponse\>
  - `getAnyOrderById(UUID orderId)` → OrderResponse
  - `cancelAnyOrder(UUID orderId)` → OrderResponse
  - `updateOrderStatusToPaid(UUID orderId)` — called by PaymentService
- [x] `com.orderhub.orders.service.OrderServiceImpl.java`:
  - Injects: `OrderRepository`, `ProductRepository` (or `ProductService`), `InventoryService`, `UserRepository`
  - **createOrder** (`@Transactional`):
    1. Load user by userId
    2. For each item in request:
       a. Load Product by productId → throw `ResourceNotFoundException` if not found
       b. Validate product is active → throw `InvalidOrderStateException` if inactive
       c. Call `inventoryService.decrementStock(productId, quantity)` → throws `InsufficientStockException` on failure
       d. Create OrderItem with snapshot price: `unitPrice = product.getPrice()`, `subtotal = unitPrice * quantity`
    3. Calculate totalAmount = sum of all subtotals
    4. Create Order entity (status=CONFIRMED, totalAmount, items)
    5. Save and return OrderResponse
    6. If any step fails → `@Transactional` rolls back everything (including inventory decrements since they're in the same transaction)
  - **cancelOrder** (`@Transactional`):
    1. Find order by id + userId → throw `ResourceNotFoundException`
    2. Validate status == CONFIRMED → throw `InvalidOrderStateException` if PAID or CANCELLED
    3. For each order item: call `inventoryService.restoreStock(productId, quantity)`
    4. Set status = CANCELLED, save
  - **cancelAnyOrder** (admin): same logic without userId check
  - **updateOrderStatusToPaid** (`@Transactional`):
    1. Find order by id → throw `ResourceNotFoundException`
    2. Validate status == CONFIRMED → throw `InvalidOrderStateException`
    3. Set status = PAID, save

### 9.5 Controllers

- [x] `com.orderhub.orders.controller.OrderController.java` (authenticated):
  - `POST /api/v1/orders` → 201 + OrderResponse
    - Extract userId from SecurityContext
  - `GET /api/v1/orders?page=0&size=10&status=CONFIRMED&createdAfter=2026-01-01&createdBefore=2026-12-31`
    - Returns only current user's orders
  - `GET /api/v1/orders/{id}` → OrderResponse (own order only, else 404)
  - `POST /api/v1/orders/{id}/cancel` → OrderResponse (own order only)
- [x] `com.orderhub.orders.controller.AdminOrderController.java` (admin):
  - `GET /api/v1/admin/orders?page=0&size=10&status=&createdAfter=&createdBefore=`
    - Returns all orders across all users
  - `GET /api/v1/admin/orders/{id}` → OrderResponse (any order)
  - `POST /api/v1/admin/orders/{id}/cancel` → OrderResponse (any CONFIRMED order)

## Implementation Notes

- **Transaction boundary**: `createOrder` is the critical atomic operation. All inventory decrements happen within one `@Transactional` method. If the last item fails, the entire transaction rolls back, restoring all previously decremented stock automatically.
- **Cancel is NOT a rollback**: Cancellation explicitly calls `restoreStock` because it happens in a separate transaction from the original order creation.
- **Product price snapshot**: `unitPrice` on OrderItem captures the price at order time. If the product price changes later, existing orders are not affected.

## Verification

- [x] `POST /api/v1/orders` with valid items → 201, order CONFIRMED, inventory decremented
- [x] `POST /api/v1/orders` with inactive product → error (409 or 400)
- [x] `POST /api/v1/orders` with insufficient stock → 409 with product details
- [x] `POST /api/v1/orders` with multiple items, one insufficient → entire order rejected, no inventory changed
- [x] `GET /api/v1/orders` returns only current user's orders
- [x] `GET /api/v1/orders/{id}` with other user's order → 404
- [x] `POST /api/v1/orders/{id}/cancel` on CONFIRMED order → CANCELLED, inventory restored
- [x] `POST /api/v1/orders/{id}/cancel` on PAID order → 409
- [x] `GET /api/v1/admin/orders` as ADMIN → returns all orders
- [x] `POST /api/v1/admin/orders/{id}/cancel` cancels any user's CONFIRMED order
- [x] Concurrent orders for last unit of product: one succeeds, other gets 409

## Files Created

```
backend/src/main/java/com/orderhub/orders/
├── controller/
│   ├── OrderController.java
│   └── AdminOrderController.java
├── dto/
│   ├── CreateOrderRequest.java
│   ├── OrderItemRequest.java
│   ├── OrderResponse.java
│   └── OrderItemResponse.java
├── entity/
│   ├── Order.java
│   ├── OrderItem.java
│   └── OrderStatus.java
├── repository/
│   └── OrderRepository.java
└── service/
    ├── OrderService.java
    └── OrderServiceImpl.java
```
