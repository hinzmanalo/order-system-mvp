# Feature 07: Catalog Backend

**Priority**: Core
**Dependencies**: 05-common-module, 06-auth-backend
**Parallel with**: None
**Blocks**: 08-inventory-backend, 09-orders-backend, 13-frontend-auth-catalog

---

## Overview

Implement the product catalog module: product entity, CRUD operations for admins, public browsing with pagination/filtering for customers, and SKU uniqueness enforcement with optimistic locking.

## User Stories

- US-005: Browse products
- US-006: View product detail
- US-014: Admin create product
- US-015: Admin update product
- US-016: Admin activate/deactivate product

## Tasks

### 7.1 Entity

- [x] `com.orderhub.catalog.entity.Product.java`:
  - `@Table(name = "products")`
  - `id` UUID, `@Id @GeneratedValue(strategy = GenerationType.UUID)`
  - `name` String NOT NULL
  - `description` String (nullable)
  - `price` BigDecimal NOT NULL
  - `sku` String, `@Column(unique = true, nullable = false)`
  - `active` boolean, default true
  - `version` int, `@Version`
  - `createdAt`, `updatedAt` LocalDateTime

### 7.2 Repository

- [x] `com.orderhub.catalog.repository.ProductRepository.java`:
  - Extends `JpaRepository<Product, UUID>`
  - `boolean existsBySku(String sku)`
  - `boolean existsBySkuAndIdNot(String sku, UUID id)`
  - Custom `@Query` for filtered active product search:
    - Filter by `active = true`
    - Optional name filter: `LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))`
    - Optional price range: `p.price >= :minPrice AND p.price <= :maxPrice`
    - Returns `Page<Product>`

### 7.3 DTOs

- [x] `ProductRequest.java`:
  - `@NotBlank` name
  - description (optional)
  - `@NotNull @Positive` price (BigDecimal)
  - `@NotBlank` sku
- [x] `ProductResponse.java`:
  - id, name, description, price, sku, active, createdAt
- [x] `ProductStatusRequest.java`:
  - `@NotNull` active (Boolean)

### 7.4 Service

- [x] `com.orderhub.catalog.service.ProductService.java` — interface:
  - `getActiveProducts(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable)` → Page\<ProductResponse\>
  - `getProductById(UUID id)` → ProductResponse
  - `createProduct(ProductRequest)` → ProductResponse
  - `updateProduct(UUID id, ProductRequest)` → ProductResponse
  - `updateProductStatus(UUID id, ProductStatusRequest)` → ProductResponse
- [x] `com.orderhub.catalog.service.ProductServiceImpl.java`:
  - **getActiveProducts**: query with optional name/price filters, map to DTO
  - **getProductById**: find or throw `ResourceNotFoundException`
  - **createProduct**:
    1. Check `existsBySku(sku)` → throw `DuplicateResourceException` if true
    2. Create and save Product entity
    3. Create associated Inventory record with quantity=0 (via InventoryRepository or InventoryService) — **TODO: Pending Phase 08**
    4. Return ProductResponse
  - **updateProduct**: find product, check SKU uniqueness (excluding self), update fields, save
  - **updateProductStatus**: find product, set active flag, save

### 7.5 Controllers

- [x] `com.orderhub.catalog.controller.ProductController.java` (public):
  - `GET /api/v1/products?page=0&size=20&name=&minPrice=&maxPrice=&sort=name,asc`
    - All params optional, paginated Spring Page response
  - `GET /api/v1/products/{id}` → single product detail
- [x] `com.orderhub.catalog.controller.AdminProductController.java` (admin):
  - `POST /api/v1/admin/products` → 201 + ProductResponse
  - `PUT /api/v1/admin/products/{id}` → 200 + ProductResponse
  - `PATCH /api/v1/admin/products/{id}/status` → 200 + ProductResponse

## Implementation Notes

- Product creation must also create an Inventory record. This creates a dependency on the Inventory entity/repository. Two approaches:
  1. Inject `InventoryRepository` directly into `ProductServiceImpl` (simpler)
  2. Call `InventoryService.createForProduct()` (cleaner module boundary)
- Option 1 is acceptable for MVP since they share the same database and JVM.

## Verification

- [x] `GET /api/v1/products` returns paginated active products (no auth required)
- [x] `GET /api/v1/products?name=phone` filters by name (case-insensitive)
- [x] `GET /api/v1/products?minPrice=50&maxPrice=500` filters by price range
- [x] `GET /api/v1/products?sort=price,desc` sorts correctly
- [x] `GET /api/v1/products/{id}` returns single product
- [x] `GET /api/v1/products/{non-existent-id}` returns 404
- [x] `POST /api/v1/admin/products` as ADMIN → 201, creates product (inventory pending Phase 08)
- [x] `POST /api/v1/admin/products` with duplicate SKU → 409
- [x] `POST /api/v1/admin/products` as USER → 403
- [x] `PUT /api/v1/admin/products/{id}` updates product fields
- [x] `PATCH /api/v1/admin/products/{id}/status` with `{"active": false}` deactivates product
- [x] Deactivated product no longer appears in `GET /api/v1/products`

## Files Created

```
backend/src/main/java/com/orderhub/catalog/
├── controller/
│   ├── ProductController.java
│   └── AdminProductController.java
├── dto/
│   ├── ProductRequest.java
│   ├── ProductResponse.java
│   └── ProductStatusRequest.java
├── entity/
│   └── Product.java
├── repository/
│   └── ProductRepository.java
└── service/
    ├── ProductService.java
    └── ProductServiceImpl.java
```
