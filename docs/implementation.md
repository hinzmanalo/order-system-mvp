# Implementation Plan: OrderHub MVP

## Overview

This document outlines the phased implementation plan for OrderHub, a modular monolith ordering system. The project starts from zero — only documentation exists. Implementation is organized into sequential phases with clear dependencies.

**Estimated phases**: 8
**Stack**: Java 17 / Spring Boot 3 backend, Angular 17+ frontend, PostgreSQL 16, Docker Compose

---

## Phase 1: Project Scaffolding & Infrastructure

**Goal**: Set up the monorepo structure, build tools, and local development environment.

### 1.1 Backend Maven project

- [ ] Create `backend/pom.xml` with dependencies:
  - Spring Boot 3.x parent
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-validation
  - spring-boot-starter-actuator
  - postgresql driver
  - flyway-core
  - jjwt (io.jsonwebtoken) — jjwt-api, jjwt-impl, jjwt-jackson
  - springdoc-openapi-starter-webmvc-ui 2.x
  - lombok (optional, or use records)
  - spring-boot-starter-test
  - testcontainers (postgresql module)
  - assertj-core
- [ ] Create `backend/src/main/java/com/orderhub/OrderHubApplication.java` — main class
- [ ] Create `backend/src/main/resources/application.yml` — default config
  - Server port 8080
  - JPA/Hibernate properties (ddl-auto=validate, show-sql=false)
  - Flyway enabled
  - Actuator endpoints (health exposed)
  - JWT config placeholders (secret, access-expiry, refresh-expiry)
- [ ] Create `backend/src/main/resources/application-dev.yml` — dev profile overrides
  - PostgreSQL connection: `jdbc:postgresql://localhost:5432/orderhub`
  - Credentials: orderhub/orderhub
  - show-sql=true for debugging
  - JWT secret (dev-only value)

### 1.2 Docker Compose

- [ ] Create `docker-compose.yml` at project root
  - `db` service: postgres:16-alpine, port 5432, health check
  - `app` service: builds `./backend`, port 8080, depends on db healthy
  - `pgdata` volume for persistence
- [ ] Create `backend/Dockerfile`
  - Multi-stage build: Maven build stage → Eclipse Temurin 17 runtime stage
  - Copy JAR, expose 8080, ENTRYPOINT

### 1.3 Angular project

- [ ] Initialize Angular 17+ project in `frontend/` via `ng new` (standalone components, no NgModules)
  - SCSS styling
  - Router enabled
  - SSR disabled (SPA only)
- [ ] Configure `frontend/proxy.conf.json` — proxy `/api` requests to `http://localhost:8080`
- [ ] Update `angular.json` to use proxy config for `ng serve`
- [ ] Verify `ng serve` runs on port 4200

### 1.4 Git & project files

- [ ] Update `.gitignore` to include:
  - `backend/target/`
  - `frontend/node_modules/`, `frontend/dist/`, `frontend/.angular/`
  - `.env` files
  - IDE files (`.idea/`, `.vscode/` if not already)

### 1.5 Verification

- [ ] `docker compose up db` starts PostgreSQL successfully
- [ ] `mvn clean compile` in `backend/` succeeds
- [ ] `ng serve` in `frontend/` serves on localhost:4200
- [ ] Spring Boot app starts and connects to PostgreSQL (Actuator `/actuator/health` returns UP)

---

## Phase 2: Database Schema (Flyway Migrations)

**Goal**: Create all database tables via versioned Flyway migration scripts.

**Directory**: `backend/src/main/resources/db/migration/`

### 2.1 Migration scripts

- [ ] `V1__create_users_table.sql`
  - `users` table: id (UUID PK), email (UNIQUE), password_hash, first_name, last_name, role (DEFAULT 'USER'), created_at, updated_at
  - UUID generation via `gen_random_uuid()`
- [ ] `V2__create_products_table.sql`
  - `products` table: id (UUID PK), name, description (TEXT), price (DECIMAL 12,2, CHECK > 0), sku (UNIQUE), active (DEFAULT TRUE), version (DEFAULT 0), created_at, updated_at
- [ ] `V3__create_inventory_table.sql`
  - `inventory` table: id (UUID PK), product_id (FK UNIQUE), quantity (DEFAULT 0, CHECK >= 0), version (DEFAULT 0), updated_at
- [ ] `V4__create_orders_tables.sql`
  - `orders` table: id (UUID PK), user_id (FK), status (DEFAULT 'CONFIRMED'), total_amount (DECIMAL 12,2), created_at, updated_at
  - Indexes: idx_orders_user_id, idx_orders_status, idx_orders_created_at
  - `order_items` table: id (UUID PK), order_id (FK CASCADE), product_id (FK), quantity (CHECK > 0), unit_price, subtotal
  - Index: idx_order_items_order_id
- [ ] `V5__create_payments_table.sql`
  - `payments` table: id (UUID PK), order_id (FK), amount, status, idempotency_key (UNIQUE), gateway_reference, created_at
  - Indexes: idx_payments_order_id, idx_payments_idempotency_key
- [ ] `V6__create_refresh_tokens_table.sql`
  - `refresh_tokens` table: id (UUID PK), user_id (FK CASCADE), token (UNIQUE), expires_at, created_at
  - Index: idx_refresh_tokens_token
- [ ] `V7__seed_dev_data.sql`
  - Admin user: admin@orderhub.com / admin123 (BCrypt hash), role=ADMIN
  - Test user: user@orderhub.com / user123 (BCrypt hash), role=USER
  - 5 sample products with inventory (50-100 units each)

### 2.2 Verification

- [ ] App starts cleanly with `flyway.enabled=true` — all migrations apply
- [ ] Tables exist with correct schema (verify via psql or pgAdmin)
- [ ] Seed data is present (2 users, 5 products, 5 inventory records)

---

## Phase 3: Common Module & Auth Module (Backend)

**Goal**: Implement shared infrastructure, exception handling, JWT security, and user authentication.

### 3.1 Common module (`com.orderhub.common`)

- [ ] `config/CorsConfig.java` — Allow `http://localhost:4200` with credentials
- [ ] `config/OpenApiConfig.java` — springdoc configuration, group endpoints by tag
- [ ] `exception/ResourceNotFoundException.java` — extends RuntimeException
- [ ] `exception/DuplicateResourceException.java`
- [ ] `exception/InsufficientStockException.java`
- [ ] `exception/InvalidOrderStateException.java`
- [ ] `exception/PaymentAmountMismatchException.java`
- [ ] `exception/GlobalExceptionHandler.java` — @RestControllerAdvice
  - RFC 7807 ProblemDetail responses for all exception types
  - Handle MethodArgumentNotValidException (400), AccessDeniedException (403), OptimisticLockingFailureException (409)
- [ ] `dto/PageResponse.java` — Generic wrapper or rely on Spring's Page directly

### 3.2 Auth entities & repositories

- [ ] `auth/entity/Role.java` — enum: USER, ADMIN
- [ ] `auth/entity/User.java` — JPA entity mapping to `users` table
  - UUID id with @GeneratedValue
  - @Enumerated(STRING) role
  - created_at, updated_at with @CreationTimestamp, @UpdateTimestamp
- [ ] `auth/entity/RefreshToken.java` — JPA entity mapping to `refresh_tokens` table
  - ManyToOne relationship to User
  - expires_at field
- [ ] `auth/repository/UserRepository.java`
  - `findByEmail(String email)` → Optional<User>
  - `existsByEmail(String email)` → boolean
- [ ] `auth/repository/RefreshTokenRepository.java`
  - `findByToken(String token)` → Optional<RefreshToken>
  - `deleteByUser(User user)` — for refresh token rotation

### 3.3 JWT security infrastructure

- [ ] `auth/security/JwtTokenProvider.java`
  - `generateAccessToken(User user)` — 15 min expiry, claims: userId, email, role
  - `generateRefreshToken()` — random secure string
  - `validateToken(String token)` → boolean
  - `getUserIdFromToken(String token)` → UUID
  - Uses jjwt library with HS256 signing
- [ ] `auth/security/CustomUserDetailsService.java`
  - Implements UserDetailsService
  - Loads user by email (for Spring Security integration)
- [ ] `auth/security/JwtAuthenticationFilter.java`
  - Extends OncePerRequestFilter
  - Extracts Bearer token from Authorization header
  - Validates token, loads user, sets SecurityContext
  - Skips filter for public endpoints
- [ ] `auth/security/JwtAuthEntryPoint.java`
  - Implements AuthenticationEntryPoint
  - Returns 401 JSON response for unauthenticated requests
- [ ] `auth/security/SecurityConfig.java`
  - @EnableWebSecurity, @EnableMethodSecurity
  - SecurityFilterChain bean:
    - Public: POST /api/v1/auth/register, /login, /refresh; GET /api/v1/products/**; GET /swagger-ui/**, /v3/api-docs/**; GET /actuator/health
    - Admin: /api/v1/admin/** requires ADMIN role
    - Authenticated: all other /api/v1/** endpoints
  - CSRF disabled (stateless JWT)
  - Session management: STATELESS
  - Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
  - PasswordEncoder bean (BCryptPasswordEncoder)
  - AuthenticationManager bean

### 3.4 Auth DTOs

- [ ] `auth/dto/RegisterRequest.java` — @NotBlank email, password (min 6), firstName, lastName; @Email validation
- [ ] `auth/dto/LoginRequest.java` — @NotBlank email, password
- [ ] `auth/dto/RefreshRequest.java` — @NotBlank refreshToken
- [ ] `auth/dto/TokenResponse.java` — accessToken, refreshToken, tokenType ("Bearer"), expiresIn (900)
- [ ] `auth/dto/UserResponse.java` — id, email, firstName, lastName, role, createdAt
- [ ] `auth/dto/UpdateRoleRequest.java` — @NotNull role (string)

### 3.5 Auth service

- [ ] `auth/service/AuthService.java` — interface
  - `register(RegisterRequest)` → UserResponse
  - `login(LoginRequest)` → TokenResponse
  - `refresh(RefreshRequest)` → TokenResponse
  - `getCurrentUser(UUID userId)` → UserResponse
  - `updateUserRole(UUID userId, UpdateRoleRequest)` → UserResponse
- [ ] `auth/service/AuthServiceImpl.java` — implementation
  - Register: check duplicate email, hash password, save user, return response
  - Login: authenticate credentials, generate tokens, save refresh token to DB
  - Refresh: validate refresh token, check expiry, issue new token pair, delete old refresh token (rotation)
  - Role update: find user, update role, save

### 3.6 Auth controllers

- [ ] `auth/controller/AuthController.java`
  - `POST /api/v1/auth/register` — public
  - `POST /api/v1/auth/login` — public
  - `POST /api/v1/auth/refresh` — public
  - `GET /api/v1/auth/me` — authenticated, returns current user from SecurityContext
- [ ] `auth/controller/AdminUserController.java`
  - `GET /api/v1/admin/users` — paginated user list
  - `GET /api/v1/admin/users/{id}` — user detail
  - `PUT /api/v1/admin/users/{id}/role` — promote user

### 3.7 Verification

- [ ] Register a new user → 201 with user details
- [ ] Login → 200 with tokens
- [ ] Access /auth/me with Bearer token → 200
- [ ] Access /auth/me without token → 401
- [ ] Access /admin/** as USER → 403
- [ ] Refresh token works and old token is invalidated
- [ ] Swagger UI accessible at /swagger-ui.html

---

## Phase 4: Catalog & Inventory Modules (Backend)

**Goal**: Implement product CRUD and inventory management.

### 4.1 Catalog entities & repository

- [ ] `catalog/entity/Product.java` — JPA entity
  - UUID id, name, description, price (BigDecimal), sku, active (boolean), version (@Version), created_at, updated_at
- [ ] `catalog/repository/ProductRepository.java`
  - Extends JpaRepository<Product, UUID>
  - Custom query: `findByActiveTrue(Pageable)` — returns Page<Product>
  - Custom query: filter by name (LIKE), price range, with active=true
  - `existsBySku(String sku)` → boolean
  - `existsBySkuAndIdNot(String sku, UUID id)` → boolean (for update uniqueness check)

### 4.2 Catalog DTOs

- [ ] `catalog/dto/ProductRequest.java` — @NotBlank name, description (optional), @NotNull @Positive price, @NotBlank sku
- [ ] `catalog/dto/ProductResponse.java` — id, name, description, price, sku, active, createdAt
- [ ] `catalog/dto/ProductStatusRequest.java` — @NotNull active (boolean)

### 4.3 Catalog service

- [ ] `catalog/service/ProductService.java` — interface
  - `getActiveProducts(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable)` → Page<ProductResponse>
  - `getProductById(UUID id)` → ProductResponse
  - `createProduct(ProductRequest)` → ProductResponse
  - `updateProduct(UUID id, ProductRequest)` → ProductResponse
  - `updateProductStatus(UUID id, ProductStatusRequest)` → ProductResponse
- [ ] `catalog/service/ProductServiceImpl.java`
  - Browse: query active products with optional name/price filters
  - Create: check SKU uniqueness, save product, auto-create inventory record (quantity=0)
  - Update: find product, check SKU uniqueness (excluding self), update fields
  - Status: find product, set active flag

### 4.4 Catalog controllers

- [ ] `catalog/controller/ProductController.java` — public endpoints
  - `GET /api/v1/products` — paginated, filterable (name, minPrice, maxPrice, sort)
  - `GET /api/v1/products/{id}` — product detail
- [ ] `catalog/controller/AdminProductController.java` — admin endpoints
  - `POST /api/v1/admin/products` — create product
  - `PUT /api/v1/admin/products/{id}` — update product
  - `PATCH /api/v1/admin/products/{id}/status` — activate/deactivate

### 4.5 Inventory entities & repository

- [ ] `inventory/entity/Inventory.java` — JPA entity
  - UUID id, product (ManyToOne), quantity, version (@Version), updated_at
- [ ] `inventory/repository/InventoryRepository.java`
  - `findByProductId(UUID productId)` → Optional<Inventory>
  - Pageable query joining product for listing

### 4.6 Inventory DTOs

- [ ] `inventory/dto/InventoryResponse.java` — productId, productName, quantity, updatedAt
- [ ] `inventory/dto/SetStockRequest.java` — @NotNull @Min(0) quantity
- [ ] `inventory/dto/AdjustStockRequest.java` — @NotNull adjustment (Integer, can be negative)

### 4.7 Inventory service

- [ ] `inventory/service/InventoryService.java` — interface
  - `getAllInventory(Pageable)` → Page<InventoryResponse>
  - `getInventoryByProductId(UUID productId)` → InventoryResponse
  - `setStock(UUID productId, SetStockRequest)` → InventoryResponse
  - `adjustStock(UUID productId, AdjustStockRequest)` → InventoryResponse
  - `decrementStock(UUID productId, int quantity)` — used by OrderService (transactional participant)
  - `restoreStock(UUID productId, int quantity)` — used by OrderService on cancellation
- [ ] `inventory/service/InventoryServiceImpl.java`
  - Set stock: find by productId, set absolute quantity, save
  - Adjust: find by productId, add delta, check >= 0, save
  - Decrement: find by productId, check sufficient stock, subtract, save (throws InsufficientStockException)
  - Restore: find by productId, add quantity back, save
  - Optimistic locking via @Version — on conflict, Spring throws OptimisticLockingFailureException

### 4.8 Inventory controller

- [ ] `inventory/controller/AdminInventoryController.java`
  - `GET /api/v1/admin/inventory` — paginated inventory list
  - `GET /api/v1/admin/inventory/{productId}` — single product inventory
  - `PUT /api/v1/admin/inventory/{productId}` — set stock
  - `PATCH /api/v1/admin/inventory/{productId}/adjust` — adjust stock

### 4.9 Verification

- [ ] GET /products returns paginated active products
- [ ] GET /products with name/price filters works correctly
- [ ] Admin CRUD on products works (create, update, deactivate)
- [ ] SKU uniqueness enforced (409 on duplicate)
- [ ] Inventory set/adjust works correctly
- [ ] Inventory cannot go below zero (409)
- [ ] Optimistic locking on concurrent product/inventory updates

---

## Phase 5: Orders & Payments Modules (Backend)

**Goal**: Implement the order lifecycle and payment processing — the most complex backend logic.

### 5.1 Order entities & repository

- [ ] `orders/entity/OrderStatus.java` — enum: CONFIRMED, PAID, CANCELLED
- [ ] `orders/entity/Order.java` — JPA entity
  - UUID id, user (ManyToOne), status (@Enumerated STRING), totalAmount (BigDecimal), items (OneToMany), created_at, updated_at
- [ ] `orders/entity/OrderItem.java` — JPA entity
  - UUID id, order (ManyToOne), product (ManyToOne), quantity, unitPrice (BigDecimal), subtotal (BigDecimal)
- [ ] `orders/repository/OrderRepository.java`
  - `findByUserId(UUID userId, Pageable)` → Page<Order>
  - `findByIdAndUserId(UUID id, UUID userId)` → Optional<Order>
  - Custom query: filter by status, date range (createdAfter/createdBefore), with pagination

### 5.2 Order DTOs

- [ ] `orders/dto/OrderItemRequest.java` — @NotNull productId (UUID), @NotNull @Min(1) quantity
- [ ] `orders/dto/CreateOrderRequest.java` — @NotEmpty @Valid List<OrderItemRequest> items
- [ ] `orders/dto/OrderResponse.java` — id, status, items (list), totalAmount, createdAt
- [ ] `orders/dto/OrderItemResponse.java` — productId, productName, quantity, unitPrice, subtotal

### 5.3 Order service

- [ ] `orders/service/OrderService.java` — interface
  - `createOrder(UUID userId, CreateOrderRequest)` → OrderResponse
  - `getOrderById(UUID orderId, UUID userId)` → OrderResponse
  - `getUserOrders(UUID userId, String status, LocalDateTime after, LocalDateTime before, Pageable)` → Page<OrderResponse>
  - `cancelOrder(UUID orderId, UUID userId)` → OrderResponse
  - `getAllOrders(String status, LocalDateTime after, LocalDateTime before, Pageable)` → Page<OrderResponse> (admin)
  - `getAnyOrderById(UUID orderId)` → OrderResponse (admin)
  - `cancelAnyOrder(UUID orderId)` → OrderResponse (admin)
  - `updateOrderStatusToPaid(UUID orderId)` — called by PaymentService
- [ ] `orders/service/OrderServiceImpl.java`
  - **Create order** (single @Transactional method):
    1. Validate all products exist and are active (via ProductService/ProductRepository)
    2. For each line item: call InventoryService.decrementStock (optimistic lock)
    3. Snapshot unit prices from product at order time
    4. Calculate subtotals and total
    5. Create Order entity (status=CONFIRMED) with OrderItem entities
    6. Save and return
    7. If any step fails → transaction rolls back (stock auto-restored by rollback)
  - **Cancel order**:
    1. Find order, verify ownership (or admin), verify status=CONFIRMED
    2. For each line item: call InventoryService.restoreStock
    3. Set status=CANCELLED, save
  - **Status update to PAID**: find order, verify status=CONFIRMED, set PAID

### 5.4 Order controllers

- [ ] `orders/controller/OrderController.java` — authenticated endpoints
  - `POST /api/v1/orders` — create order (extract userId from SecurityContext)
  - `GET /api/v1/orders` — list own orders (paginated, filterable by status/date)
  - `GET /api/v1/orders/{id}` — get own order detail
  - `POST /api/v1/orders/{id}/cancel` — cancel own order
- [ ] `orders/controller/AdminOrderController.java` — admin endpoints
  - `GET /api/v1/admin/orders` — list all orders
  - `GET /api/v1/admin/orders/{id}` — get any order detail
  - `POST /api/v1/admin/orders/{id}/cancel` — cancel any order

### 5.5 Payment entities & repository

- [ ] `payments/entity/PaymentStatus.java` — enum: SUCCESS, FAILED
- [ ] `payments/entity/Payment.java` — JPA entity
  - UUID id, order (ManyToOne), amount (BigDecimal), status, idempotencyKey, gatewayReference, created_at
- [ ] `payments/repository/PaymentRepository.java`
  - `findByIdempotencyKey(String key)` → Optional<Payment>
  - `findByOrderId(UUID orderId)` → List<Payment>

### 5.6 Payment gateway

- [ ] `payments/gateway/PaymentGateway.java` — interface
  - `processPayment(BigDecimal amount, String referenceId)` → PaymentGatewayResult
- [ ] `payments/gateway/PaymentGatewayResult.java` — record/class: success (boolean), gatewayReference (String)
- [ ] `payments/gateway/MockPaymentGateway.java` — @Component implementation
  - 90% success rate (Random)
  - Generates "MOCK-REF-{uuid}" as gateway reference on success

### 5.7 Payment DTOs

- [ ] `payments/dto/PaymentRequest.java` — @NotNull @Positive amount (BigDecimal)
- [ ] `payments/dto/PaymentResponse.java` — id, orderId, amount, status, gatewayReference, createdAt

### 5.8 Payment service

- [ ] `payments/service/PaymentService.java` — interface
  - `processPayment(UUID orderId, UUID userId, String idempotencyKey, PaymentRequest)` → PaymentResponse
  - `getPaymentsByOrderId(UUID orderId, UUID userId)` → List<PaymentResponse>
- [ ] `payments/service/PaymentServiceImpl.java`
  - **Process payment** (@Transactional):
    1. Check for existing payment with same idempotency key
       - If found and SUCCESS → return existing result (idempotent)
       - If found and FAILED → allow reprocessing with new idempotency key (not same key)
    2. Load order, verify ownership, verify status=CONFIRMED
    3. Validate amount == order.totalAmount (throw PaymentAmountMismatchException)
    4. Call PaymentGateway.processPayment
    5. Create Payment entity (SUCCESS or FAILED)
    6. If SUCCESS → call OrderService.updateOrderStatusToPaid
    7. Save and return

### 5.9 Payment controller

- [ ] `payments/controller/PaymentController.java`
  - `POST /api/v1/orders/{orderId}/payments` — process payment
    - Extract `Idempotency-Key` from request header (@RequestHeader)
    - Validate header is present (400 if missing)
  - `GET /api/v1/orders/{orderId}/payments` — get payment status/history for order

### 5.10 Verification

- [ ] Full order lifecycle: create → pay → PAID status
- [ ] Full order lifecycle: create → cancel → CANCELLED, inventory restored
- [ ] Insufficient stock returns 409 with descriptive message
- [ ] Payment amount mismatch returns 422
- [ ] Paying a non-CONFIRMED order returns 409
- [ ] Idempotency key prevents duplicate payments
- [ ] Missing idempotency key returns 400
- [ ] Concurrent orders for last item: one succeeds, other gets 409

---

## Phase 6: Backend Testing

**Goal**: Achieve >80% service-layer test coverage with unit and integration tests.

### 6.1 Test infrastructure

- [ ] Add Testcontainers PostgreSQL dependency to pom.xml (test scope)
- [ ] Create abstract base integration test class with @Testcontainers setup
  - PostgreSQL container configuration
  - Dynamic datasource properties via @DynamicPropertySource

### 6.2 Unit tests (JUnit 5 + Mockito)

- [ ] `auth/service/AuthServiceTest.java`
  - Test successful registration
  - Test duplicate email rejection
  - Test successful login with valid credentials
  - Test login with invalid credentials
  - Test token refresh with valid/expired/invalid refresh token
  - Test role update (USER→ADMIN, already ADMIN idempotent)
- [ ] `catalog/service/ProductServiceTest.java`
  - Test product creation with auto-inventory
  - Test product update
  - Test SKU uniqueness enforcement
  - Test product status toggle (activate/deactivate)
  - Test active product listing with filters
  - Test product not found
- [ ] `inventory/service/InventoryServiceTest.java`
  - Test set stock (absolute value)
  - Test adjust stock (positive and negative delta)
  - Test adjust stock below zero fails
  - Test decrement stock (sufficient and insufficient)
  - Test restore stock
  - Test optimistic lock conflict handling
- [ ] `orders/service/OrderServiceTest.java`
  - Test successful order creation (multiple items)
  - Test order creation with inactive product fails
  - Test order creation with insufficient stock fails (full rollback)
  - Test cancel own order (CONFIRMED)
  - Test cancel PAID order fails
  - Test user can only see own orders
  - Test order status transitions
- [ ] `payments/service/PaymentServiceTest.java`
  - Test successful payment processing
  - Test failed payment (gateway returns failure)
  - Test idempotency (same key returns existing result)
  - Test amount mismatch
  - Test payment on non-CONFIRMED order
  - Test missing idempotency key

### 6.3 Integration tests (SpringBootTest + Testcontainers)

- [ ] `auth/controller/AuthControllerIntegrationTest.java`
  - End-to-end: register → login → access protected resource
  - Token refresh flow
  - Invalid credentials handling
  - Admin endpoint access control
- [ ] `catalog/controller/ProductControllerIntegrationTest.java`
  - Product CRUD via HTTP
  - Pagination and filter params
  - Admin-only access enforcement
- [ ] `orders/controller/OrderControllerIntegrationTest.java`
  - Full lifecycle: create order → verify inventory decremented → pay → verify PAID
  - Full lifecycle: create order → cancel → verify inventory restored
  - Insufficient stock scenario
- [ ] `payments/controller/PaymentControllerIntegrationTest.java`
  - Payment with idempotency key
  - Amount mismatch
  - Invalid order state
- [ ] `inventory/repository/InventoryRepositoryTest.java`
  - Optimistic locking under concurrent updates (use ExecutorService with multiple threads)

### 6.4 Verification

- [ ] All unit tests pass: `mvn test`
- [ ] All integration tests pass: `mvn verify`
- [ ] Coverage report shows >80% on service layer

---

## Phase 7: Frontend Implementation (Angular 17+)

**Goal**: Build the Angular SPA with all customer and admin features.

### 7.1 Core infrastructure

- [ ] **Models** (`core/models/`)
  - `user.model.ts` — User, LoginRequest, RegisterRequest, TokenResponse interfaces
  - `product.model.ts` — Product, ProductPage interfaces
  - `order.model.ts` — Order, OrderItem, CreateOrderRequest interfaces
  - `payment.model.ts` — Payment, PaymentRequest interfaces
  - `cart.model.ts` — CartItem interface
- [ ] **Auth service** (`core/services/auth.service.ts`)
  - Signals: currentUser, isAuthenticated, isAdmin
  - Methods: register(), login(), logout(), refresh(), getMe()
  - Store access token in memory, refresh token in localStorage
  - Auto-decode JWT for role/user info
- [ ] **Auth interceptor** (`core/interceptors/auth.interceptor.ts`)
  - Functional interceptor (HttpInterceptorFn)
  - Attach Authorization header on outgoing requests
  - On 401: attempt refresh, queue concurrent requests, retry
  - On refresh failure: clear state, redirect to /login
- [ ] **Auth guard** (`core/guards/auth.guard.ts`)
  - Functional canActivate guard
  - Redirect to /login if not authenticated
- [ ] **Admin guard** (`core/guards/admin.guard.ts`)
  - Functional canActivate guard
  - Redirect to /products if not ADMIN role
- [ ] **App config** (`app.config.ts`)
  - provideRouter with routes
  - provideHttpClient with interceptor
- [ ] **App routes** (`app.routes.ts`)
  - All routes per PRD section 11.2 with lazy loading

### 7.2 Shared components

- [ ] **Navbar** (`shared/components/navbar/`)
  - App title/logo link
  - Products, Cart (with item count badge), Orders links
  - Admin link (visible only to ADMIN)
  - Login/Register or Logout (based on auth state)
  - Responsive hamburger menu for mobile
- [ ] **Toast** (`shared/components/toast/`)
  - Toast notification service + component
  - Success (green), error (red), info (blue) variants
  - Auto-dismiss after 3-5 seconds
- [ ] **Confirm dialog** (`shared/components/confirm-dialog/`)
  - Modal with message, confirm/cancel buttons
  - Used for destructive actions (cancel order)
- [ ] **Pagination** (`shared/components/pagination/`)
  - Page navigation based on Spring Page response
  - Previous/Next, page numbers
- [ ] **Currency pipe** (`shared/pipes/currency-format.pipe.ts`)
  - Format BigDecimal prices to currency display

### 7.3 API services

- [ ] **Product service** (`core/services/product.service.ts`)
  - getProducts(page, size, name, minPrice, maxPrice, sort) → Observable<ProductPage>
  - getProductById(id) → Observable<Product>
- [ ] **Cart service** (`core/services/cart.service.ts`)
  - Signals: cartItems, cartCount, cartTotal
  - Methods: addToCart(product, qty), removeFromCart(productId), updateQuantity(productId, qty), clearCart()
  - Persist to/from localStorage
- [ ] **Order service** (`core/services/order.service.ts`)
  - createOrder(items) → Observable<Order>
  - getMyOrders(page, status, dateRange) → Observable<OrderPage>
  - getOrderById(id) → Observable<Order>
  - cancelOrder(id) → Observable<Order>
- [ ] **Payment service** (`core/services/payment.service.ts`)
  - processPayment(orderId, amount) → Observable<Payment>
    - Auto-generate UUID idempotency key
  - getPayments(orderId) → Observable<Payment[]>
- [ ] **Admin service** (`core/services/admin.service.ts`)
  - Products: createProduct, updateProduct, updateProductStatus
  - Inventory: getInventory, setStock, adjustStock
  - Users: getUsers, getUserById, updateUserRole
  - Orders: getAllOrders, getOrderById, cancelOrder

### 7.4 Feature components — Auth

- [ ] **Login component** (`features/auth/login/`)
  - Reactive form: email, password
  - Validation: required fields, email format
  - Submit → AuthService.login → redirect to /products
  - Link to register page
  - Error display for invalid credentials
- [ ] **Register component** (`features/auth/register/`)
  - Reactive form: email, password, firstName, lastName
  - Validation: required, email format, password min length
  - Submit → AuthService.register → redirect to /login with success toast
  - Link to login page
  - Error display for duplicate email

### 7.5 Feature components — Catalog

- [ ] **Product list** (`features/catalog/product-list/`)
  - Product grid/cards layout
  - Search input (name filter, debounced)
  - Price range filters (min/max inputs)
  - Sort dropdown (name asc/desc, price asc/desc)
  - Pagination controls
  - "Add to Cart" button on each product card
  - Loading spinner during fetch
- [ ] **Product detail** (`features/catalog/product-detail/`)
  - Product name, description, price, SKU
  - Quantity selector + "Add to Cart" button
  - Back to products link
  - Loading spinner

### 7.6 Feature components — Cart & Checkout

- [ ] **Cart component** (`features/cart/`)
  - Table: product name, unit price, quantity (editable), subtotal, remove button
  - Cart total
  - "Proceed to Checkout" button (disabled if cart empty)
  - Empty cart message with link to /products
  - Requires authentication (authGuard)
- [ ] **Checkout component** (`features/checkout/`)
  - Order summary (read-only cart review)
  - Total amount
  - "Place Order" button
  - On success: clear cart, show order ID, navigate to order detail
  - On error: display error message (e.g., insufficient stock)
  - Loading state during order creation

### 7.7 Feature components — Orders

- [ ] **Order list** (`features/orders/order-list/`)
  - Table: order ID (truncated), status badge, item count, total, date
  - Status filter dropdown
  - Pagination
  - Click row → navigate to order detail
- [ ] **Order detail** (`features/orders/order-detail/`)
  - Order status badge (color-coded: CONFIRMED=blue, PAID=green, CANCELLED=red)
  - Line items table: product name, quantity, unit price, subtotal
  - Order total
  - Actions:
    - "Pay Now" button (visible if status=CONFIRMED)
    - "Cancel Order" button (visible if status=CONFIRMED, with confirm dialog)
  - Payment history section (if payments exist)
  - Loading state

### 7.8 Feature components — Admin

- [ ] **Admin dashboard** (`features/admin/dashboard/`)
  - Navigation cards/links to: Products, Inventory, Users, Orders
  - Could show summary counts (optional for MVP)
- [ ] **Product management** (`features/admin/products/`)
  - Table: name, SKU, price, active status, actions (edit, toggle status)
  - "Add Product" button → opens product form
  - Product form (create/edit): name, description, price, SKU fields
  - Toggle active/inactive with confirmation
  - Pagination
- [ ] **Inventory management** (`features/admin/inventory/`)
  - Table: product name, SKU, current quantity, actions
  - "Set Stock" action → input absolute quantity
  - "Adjust Stock" action → input +/- delta
  - Pagination
- [ ] **User management** (`features/admin/users/`)
  - Table: email, name, role, registration date
  - "Promote to Admin" button (for USER role, with confirmation)
  - Pagination
- [ ] **Order management** (`features/admin/orders/`)
  - Table: order ID, user email, status, total, date, actions
  - Status filter, date range filter
  - "Cancel" button for CONFIRMED orders (with confirmation)
  - Click to view order detail
  - Pagination

### 7.9 Global styles

- [ ] `styles.scss` — Global theme
  - CSS variables for colors (primary, success, danger, warning, info)
  - Status badge styles
  - Form styles
  - Table styles
  - Button variants
  - Responsive breakpoints
  - Loading spinner animation

### 7.10 Verification

- [ ] Registration → Login → Browse → Add to Cart → Checkout → Pay (full flow)
- [ ] Cancel order flow with inventory restoration
- [ ] Admin: create product → set stock → view in catalog
- [ ] Admin: manage users, view all orders, cancel orders
- [ ] Auth guard redirects to login for protected routes
- [ ] Admin guard blocks non-admin users
- [ ] Token refresh works transparently
- [ ] Responsive layout works on mobile viewport
- [ ] Toast notifications appear for success/error actions

---

## Phase 8: Integration, Polish & Documentation

**Goal**: Final integration testing, Docker Compose validation, API docs, and cleanup.

### 8.1 Docker Compose end-to-end

- [ ] Build backend Docker image successfully
- [ ] `docker compose up` starts both services
- [ ] Flyway migrations run on fresh database
- [ ] Application is accessible at localhost:8080
- [ ] Frontend proxy connects to backend API
- [ ] Full user flow works via Docker Compose

### 8.2 API documentation (Swagger/OpenAPI)

- [ ] Verify Swagger UI accessible at /swagger-ui.html
- [ ] All endpoints documented with correct request/response schemas
- [ ] Endpoints grouped by tags: Auth, Products, Inventory, Orders, Payments, Admin
- [ ] @Operation annotations with descriptions on each controller method
- [ ] @ApiResponse annotations for success and error responses

### 8.3 Actuator health check

- [ ] GET /actuator/health returns UP
- [ ] Database health indicator shows UP
- [ ] Accessible without authentication

### 8.4 Edge case handling

- [ ] Concurrent order placement handled (optimistic lock → 409 with clear message)
- [ ] Duplicate payment idempotency works correctly
- [ ] Expired tokens trigger refresh or redirect to login
- [ ] Cart with deactivated product → checkout shows clear error
- [ ] Price changes between cart and checkout → order snapshots price at order time

### 8.5 Code cleanup

- [ ] Remove unused imports and dead code
- [ ] Consistent code formatting
- [ ] Meaningful log statements at service layer (INFO for business events, WARN for failures)
- [ ] No hardcoded secrets (JWT secret from config/env variable)
- [ ] No sensitive data in API responses (no password hashes)

### 8.6 Frontend tests (Jasmine/Karma)

- [ ] Key component unit tests: login, register, product-list, cart, checkout
- [ ] Service tests with HttpClientTestingModule
- [ ] Guard tests
- [ ] `ng test` passes

### 8.7 Final verification checklist

- [ ] `docker compose up` — app starts from scratch with single command
- [ ] Seed data present (admin + user + products + inventory)
- [ ] Full customer flow: register → login → browse → cart → checkout → pay
- [ ] Full admin flow: login → manage products → manage inventory → manage users → manage orders
- [ ] All API endpoints return correct HTTP status codes
- [ ] Error responses follow RFC 7807 format
- [ ] Swagger UI shows complete API documentation
- [ ] Actuator health check returns UP
- [ ] `mvn test` — all backend tests pass
- [ ] `mvn verify` — all integration tests pass
- [ ] `ng test` — all frontend tests pass
- [ ] No security vulnerabilities (no SQL injection, XSS, exposed secrets)

---

## Dependency Graph

```
Phase 1 (Scaffolding)
  └── Phase 2 (Database Schema)
        └── Phase 3 (Common + Auth)
              └── Phase 4 (Catalog + Inventory)
                    └── Phase 5 (Orders + Payments)
                          ├── Phase 6 (Backend Testing)
                          └── Phase 7 (Frontend)
                                └── Phase 8 (Integration & Polish)
```

Each phase depends on the previous one. Phases 6 and 7 can be parallelized after Phase 5 is complete.

---

## Key Implementation Notes

1. **Transaction boundaries**: Order creation must be a single `@Transactional` method. Inventory decrements happen within the same transaction, so a failure automatically rolls back stock changes.

2. **Optimistic locking**: Use JPA `@Version` on Product and Inventory entities. Spring automatically throws `OptimisticLockingFailureException` on version mismatch. Catch this in GlobalExceptionHandler and return 409.

3. **JWT structure**: Access tokens are short-lived (15 min) JWTs containing userId, email, and role claims. Refresh tokens are opaque strings stored in the database for revocation capability.

4. **Inter-module communication**: Service-layer direct calls (not REST). For example, `OrderServiceImpl` injects `InventoryService` and `ProductService` directly. Keep interfaces between modules clean.

5. **DTO mapping**: Map entities to response DTOs in the service layer. Never expose JPA entities directly in controller responses.

6. **Idempotency**: Payment idempotency is enforced via unique constraint on `idempotency_key` in the payments table. Check for existing payment before processing.

7. **Security context**: Extract authenticated user's ID from `SecurityContextHolder` in controllers. Pass user ID to service methods for ownership verification.
