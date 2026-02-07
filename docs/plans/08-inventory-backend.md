# Feature 08: Inventory Backend

**Priority**: Core
**Dependencies**: 05-common-module, 07-catalog-backend
**Parallel with**: None
**Blocks**: 09-orders-backend, 15-frontend-admin

---

## Overview

Implement inventory management: entity, admin CRUD endpoints (set stock, adjust stock), and internal service methods for stock decrement/restore used by the orders module. Includes optimistic locking to prevent overselling.

## User Stories

- US-017: Admin set stock
- US-018: Admin adjust stock
- US-023: View inventory (admin)
- US-024 (partial): Concurrent order handling — optimistic locking on inventory

## Tasks

### 8.1 Entity

- [ ] `com.orderhub.inventory.entity.Inventory.java`:
  - `@Table(name = "inventory")`
  - `id` UUID, `@Id @GeneratedValue(strategy = GenerationType.UUID)`
  - `product` Product, `@OneToOne(fetch = LAZY)`, `@JoinColumn(name = "product_id", unique = true)`
  - `quantity` int, default 0
  - `version` int, `@Version` (optimistic locking)
  - `updatedAt` LocalDateTime, `@UpdateTimestamp`

### 8.2 Repository

- [ ] `com.orderhub.inventory.repository.InventoryRepository.java`:
  - Extends `JpaRepository<Inventory, UUID>`
  - `Optional<Inventory> findByProductId(UUID productId)`
  - `@Query` for paginated listing joining Product (to include product name/SKU in response)

### 8.3 DTOs

- [ ] `InventoryResponse.java` — productId, productName, productSku, quantity, updatedAt
- [ ] `SetStockRequest.java` — `@NotNull @Min(0)` quantity (Integer)
- [ ] `AdjustStockRequest.java` — `@NotNull` adjustment (Integer, can be negative)

### 8.4 Service

- [ ] `com.orderhub.inventory.service.InventoryService.java` — interface:
  - `getAllInventory(Pageable)` → Page\<InventoryResponse\>
  - `getInventoryByProductId(UUID productId)` → InventoryResponse
  - `setStock(UUID productId, SetStockRequest)` → InventoryResponse
  - `adjustStock(UUID productId, AdjustStockRequest)` → InventoryResponse
  - `decrementStock(UUID productId, int quantity)` — internal, called by OrderService
  - `restoreStock(UUID productId, int quantity)` — internal, called by OrderService on cancel
- [ ] `com.orderhub.inventory.service.InventoryServiceImpl.java`:
  - **getAllInventory**: query with product join, map to DTO
  - **getInventoryByProductId**: find by productId or throw `ResourceNotFoundException`
  - **setStock**: find inventory, set absolute quantity, save
  - **adjustStock**: find inventory, add delta, validate `quantity + adjustment >= 0` (throw `InsufficientStockException` if negative), save
  - **decrementStock**: find inventory, check `quantity >= requestedQty` (throw `InsufficientStockException`), subtract, save
  - **restoreStock**: find inventory, add quantity back, save

### 8.5 Controller

- [ ] `com.orderhub.inventory.controller.AdminInventoryController.java`:
  - `GET /api/v1/admin/inventory?page=0&size=20` → Page\<InventoryResponse\>
  - `GET /api/v1/admin/inventory/{productId}` → InventoryResponse
  - `PUT /api/v1/admin/inventory/{productId}` → InventoryResponse (set stock)
  - `PATCH /api/v1/admin/inventory/{productId}/adjust` → InventoryResponse (adjust stock)

## Implementation Notes

- `@Version` on the `version` field enables optimistic locking. When two concurrent requests try to update the same inventory record, the second will get `OptimisticLockingFailureException`, which is caught by `GlobalExceptionHandler` and returned as 409.
- `decrementStock` and `restoreStock` are designed to be called within the same transaction as order creation/cancellation. They don't have their own `@Transactional` — they participate in the caller's transaction.

## Verification

- [ ] `GET /api/v1/admin/inventory` as ADMIN returns paginated inventory list with product details
- [ ] `GET /api/v1/admin/inventory` as USER → 403
- [ ] `GET /api/v1/admin/inventory/{productId}` returns single inventory record
- [ ] `PUT /api/v1/admin/inventory/{productId}` with `{"quantity": 100}` sets stock to 100
- [ ] `PUT /api/v1/admin/inventory/{productId}` with `{"quantity": -1}` → 400 (validation)
- [ ] `PATCH /api/v1/admin/inventory/{productId}/adjust` with `{"adjustment": -5}` reduces stock by 5
- [ ] `PATCH /api/v1/admin/inventory/{productId}/adjust` with delta that would go below 0 → 409
- [ ] Concurrent updates: two simultaneous requests to same product → one succeeds, other gets 409

## Files Created

```
backend/src/main/java/com/orderhub/inventory/
├── controller/
│   └── AdminInventoryController.java
├── dto/
│   ├── InventoryResponse.java
│   ├── SetStockRequest.java
│   └── AdjustStockRequest.java
├── entity/
│   └── Inventory.java
├── repository/
│   └── InventoryRepository.java
└── service/
    ├── InventoryService.java
    └── InventoryServiceImpl.java
```
