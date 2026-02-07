# PRD: OrderHub - Monolith Ordering System (MVP)

- **Version**: 1.0
- **Last updated**: 2026-02-07

## 1. Product overview

### 1.1 Product summary

OrderHub is a portfolio-grade monolithic ordering system built with a modular monolith architecture. It demonstrates real-world backend engineering skills including transactional integrity, inventory control, payment processing, clean domain separation, and JWT-based security.

The system consists of a Spring Boot 3 REST API backend and an Angular 17+ single-page application frontend, packaged as a monorepo. It supports two user roles: customers who browse products, place multi-item orders, and process payments, and administrators who manage the product catalog, inventory levels, user accounts, and order oversight.

The MVP focuses on a complete order lifecycle from product browsing through payment, with atomic inventory management and a pluggable payment gateway abstraction.

### 1.2 Tech stack summary

Java 17 / Spring Boot 3 backend with PostgreSQL, Angular 17+ SPA frontend, Docker Compose for local development. See **Section 8 — Technology stack** for full details.

## 2. Goals

### 2.1 Business goals

- Demonstrate enterprise-grade backend engineering in a single deployable application
- Showcase modular monolith architecture with clean domain boundaries
- Exhibit transactional integrity, concurrency handling, and inventory consistency
- Present a production-realistic full-stack application for portfolio use

### 2.2 User goals

- Customers can browse products, place multi-item orders, pay, and track order status
- Admins can manage the product catalog, adjust inventory, manage users, and oversee orders
- All users experience secure authentication with role-appropriate access

### 2.3 Non-goals

- Real payment gateway integration (Stripe, PayPal) — mock gateway only in MVP
- Email notifications or messaging
- Microservices decomposition
- Mobile application
- Shopping cart persistence (server-side) — cart is client-side in Angular
- Product images/media upload
- Shipping/delivery tracking
- Discount codes or promotions
- Multi-currency or internationalization

## 3. User personas

### 3.1 Key user types

- **Customer** — End user who browses products, places orders, and makes payments
- **Administrator** — Internal user who manages the system (products, inventory, users, orders)

### 3.2 Basic persona details

- **Customer (Alex)**: A user who registers an account, browses the product catalog, adds items to their cart, places an order, and pays for it. Wants a straightforward shopping experience with clear order status tracking.

- **Administrator (Jordan)**: A system admin who manages the product catalog, monitors and adjusts inventory levels, handles user role management, and oversees all orders. Needs efficient tooling to manage operations.

### 3.3 Role-based access

- **USER**: Can register, login, browse active products, place orders, pay for own orders, cancel own unpaid orders, view own order history. Cannot access admin endpoints.

- **ADMIN**: Has all USER permissions plus: create/update/deactivate products, adjust inventory stock levels, view and manage all orders, promote users to ADMIN role, view all users.

## 4. Functional requirements

### 4.1 Authentication and authorization (Priority: High)

- Self-registration: anyone can register as a USER with email, password, first name, last name
- Login returns a short-lived access token (15 min) and a long-lived refresh token (7 days)
- Refresh endpoint issues a new access token using a valid refresh token
- Passwords stored as BCrypt hashes
- JWT contains user ID, email, and role claims
- Role-based endpoint protection via Spring Security filter chain
- Only ADMINs can promote a USER to ADMIN role
- Current user can retrieve their own profile

### 4.2 Product catalog (Priority: High)

- Products have: name, description, price (BigDecimal), SKU (unique), active/inactive status
- Admins create and update products
- Admins can deactivate products (soft delete — set active = false)
- Customers browse only active products
- Product listing supports:
  - Spring Page offset-based pagination (page, size, sort params)
  - Filter by name (case-insensitive LIKE/CONTAINS)
  - Filter by price range (minPrice, maxPrice)
- Single product detail retrieval by ID
- SKU must be unique across all products
- Price must be positive
- Optimistic locking via @Version for concurrent updates

### 4.3 Inventory management (Priority: High)

- Each product has an associated inventory record tracking available quantity
- Stock quantity is immediately decremented when an order is created (atomic with order creation)
- Order cancellation restores the decremented stock quantity
- Optimistic locking (@Version) on inventory records to prevent overselling under concurrency
- Admins can adjust stock levels (set absolute quantity or add/subtract delta)
- Stock cannot go below zero — order creation fails if insufficient stock for any line item
- Inventory check and decrement happen within the same transaction as order creation

### 4.4 Order management (Priority: High)

- Orders support multiple line items, each referencing a product with a quantity
- Order lifecycle states: CONFIRMED, PAID, CANCELLED
  - Order is created and auto-confirmed atomically if all items have sufficient stock
  - The system skips the intermediate CREATED state — orders enter as CONFIRMED directly
  - CONFIRMED orders can transition to PAID (via payment) or CANCELLED
  - PAID is a terminal state
  - CANCELLED is a terminal state
- Order creation is fully atomic:
  1. Validate all products exist and are active
  2. Check and decrement inventory for all line items
  3. Calculate order total (sum of unit_price * quantity for each line item)
  4. Create order with status CONFIRMED
  5. If any step fails, the entire transaction rolls back
- Unit price is captured at order time (snapshot) to protect against price changes
- Customers can view their own orders only
- Admins can view all orders
- Order listing supports:
  - Spring Page offset-based pagination
  - Filter by status
  - Filter by date range (createdAfter, createdBefore)
- Cancellation rules:
  - Customer can cancel their own order if status is CONFIRMED (not yet paid)
  - Admin can cancel any order if status is CONFIRMED
  - Cancellation restores inventory for all line items
  - PAID orders cannot be cancelled

### 4.5 Payment processing (Priority: High)

- Payment gateway uses the Strategy pattern: `PaymentGateway` interface with a `MockPaymentGateway` implementation
- Mock gateway simulates success (90%) and failure (10%) for demonstration purposes
- Payment request requires:
  - Order ID
  - Payment amount (must exactly equal the order total)
  - `Idempotency-Key` header (UUID) to prevent duplicate payment processing
- Only CONFIRMED orders can be paid
- On successful payment:
  - Payment record created with status SUCCESS and gateway reference
  - Order status transitions to PAID
- On failed payment:
  - Payment record created with status FAILED
  - Order remains in CONFIRMED state (customer can retry)
- Idempotency: if a payment with the same Idempotency-Key already exists and succeeded, return the existing payment result without reprocessing
- Payment amount mismatch returns an error without processing

### 4.6 User management — admin (Priority: Medium)

- Admin can list all users with pagination
- Admin can view individual user details
- Admin can promote a USER to ADMIN role
- Admin cannot demote (MVP simplification)
- Admin cannot delete users (MVP simplification)

## 5. User experience

### 5.1 Entry points and first-time user flow

- New user lands on the login page
- User clicks "Register" to create an account (email, password, name)
- After registration, user is redirected to login
- After login, user lands on the product listing page
- Admin users see an additional "Admin" navigation link

### 5.2 Core experience

- **Product browsing**: User sees a paginated grid/list of products with search and price filter controls. Clicking a product opens the detail view.
- **Cart management**: User adds products to a client-side cart (Angular state). Cart shows item count in the navbar. Cart page shows line items with quantities and totals. User can adjust quantities or remove items.
- **Checkout**: User reviews cart contents and total, then confirms the order. System creates the order atomically. Success shows the order confirmation with order ID.
- **Payment**: From order detail or order history, user initiates payment for a CONFIRMED order. System processes via mock gateway. Success/failure feedback is displayed immediately.
- **Order tracking**: User views order history with status badges (CONFIRMED, PAID, CANCELLED). Each order shows line items, totals, and payment status.
- **Admin dashboard**: Tabbed or sidebar navigation to Products, Inventory, Users, Orders management sections. Each section provides CRUD operations with table views, forms, and filters.

### 5.3 Advanced features and edge cases

- Concurrent order placement for same product: optimistic locking throws conflict, user sees "insufficient stock" or retry message
- Double-click on payment: idempotency key prevents duplicate charges
- Session expiry: expired access token triggers automatic refresh via HTTP interceptor. Expired refresh token redirects to login
- Cart references deactivated product: checkout validation rejects with clear message
- Price changes after adding to cart: order captures price at order time; cart shows current prices with a note if they differ

### 5.4 UI/UX highlights

- Responsive layout (desktop-first, mobile-friendly)
- Loading spinners for async operations
- Toast notifications for success/error feedback
- Confirmation dialogs for destructive actions (cancel order)
- Form validation with inline error messages
- Status badges with color coding (CONFIRMED=blue, PAID=green, CANCELLED=red)

## 6. Narrative

Alex discovers OrderHub, registers an account, and browses the product catalog. They find several items they need, add them to their cart, and proceed to checkout. The system atomically validates stock, decrements inventory, and creates a confirmed order. Alex navigates to their order and pays — the mock payment gateway processes it successfully, and the order moves to PAID status. Later, Alex places another order but decides to cancel it before paying, and the inventory is automatically restored.

Meanwhile, Jordan logs in as an admin, adds new products to the catalog, adjusts stock levels for existing items, and monitors incoming orders. Jordan notices a new user registration and promotes them to admin status to help manage the growing catalog.

## 7. Success metrics

### 7.1 User-centric metrics

- Order placement success rate (target: >95% when stock is available)
- Payment processing success rate (mock gateway: ~90% by design)
- API response time for product listing (target: <200ms for paginated queries)

### 7.2 Business metrics

- Complete order lifecycle demonstrated end-to-end
- Zero overselling incidents (inventory consistency)
- All CRUD operations functional for admin

### 7.3 Technical metrics

- Test coverage >80% on service layer
- All API endpoints documented in Swagger
- Application starts and runs via Docker Compose with a single command
- Flyway migrations run cleanly on fresh database
- Spring Boot Actuator health endpoint returns UP

## 8. Technology stack

### 8.1 Backend

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Language | Java 17 | LTS release with records, sealed classes, text blocks, pattern matching |
| Framework | Spring Boot 3.x | Convention-over-configuration web framework with embedded Tomcat |
| ORM | Spring Data JPA (Hibernate 6) | Object-relational mapping with repository pattern abstraction |
| Database | PostgreSQL 16 | ACID-compliant relational database with JSONB, full-text search support |
| Migrations | Flyway | Versioned SQL migration scripts applied automatically on startup |
| Build tool | Maven | Dependency management, build lifecycle, plugin ecosystem |
| Security | Spring Security 6 | Authentication filter chain, method-level security, CORS configuration |
| JWT | jjwt (io.jsonwebtoken) | JWT token creation, signing (HS256/RS256), parsing, and validation |
| Validation | Jakarta Bean Validation (Hibernate Validator) | Declarative request DTO validation with @Valid, @NotBlank, @Positive, etc. |
| API docs | springdoc-openapi 2.x | Auto-generates OpenAPI 3 spec from controllers; serves Swagger UI |
| Connection pool | HikariCP | High-performance JDBC connection pooling (Spring Boot default) |
| Logging | SLF4J + Logback | Structured logging with configurable levels and JSON output support |
| Monitoring | Spring Boot Actuator | Health checks (/actuator/health), metrics, info, and environment endpoints |

### 8.2 Frontend

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | Angular 17+ | SPA framework with standalone components (no NgModules) |
| Language | TypeScript 5.x | Strongly-typed JavaScript for maintainable frontend code |
| State management | Angular Signals | Built-in reactive state primitives (no NgRx needed for MVP) |
| HTTP client | Angular HttpClient | Built-in HTTP client with interceptor support for JWT attachment |
| Routing | Angular Router | Lazy-loaded routes with functional guards (authGuard, adminGuard) |
| Forms | Angular Reactive Forms | Type-safe form handling with built-in validators |
| Styling | CSS / SCSS | Component-scoped styles with global theme variables |
| Dev server | Angular CLI (ng serve) | Hot-reload development server on port 4200 with proxy to backend |

### 8.3 Infrastructure

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Containerization | Docker | Backend Dockerfile for building the Spring Boot JAR image |
| Orchestration | Docker Compose | Multi-service local environment (app + PostgreSQL) |
| Database image | postgres:16-alpine | Lightweight PostgreSQL container for development |
| Java runtime | Eclipse Temurin 17 (Docker base) | Production-grade JDK for container runtime |
| Node runtime | Node.js 20 LTS | Required for Angular CLI build and development |

### 8.4 Testing

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Unit testing | JUnit 5 (Jupiter) | Test framework for service-layer unit tests |
| Mocking | Mockito | Mock dependencies in unit tests |
| Integration testing | @SpringBootTest | Full application context testing with HTTP client |
| Test database | Testcontainers (PostgreSQL) | Disposable PostgreSQL container for integration tests |
| API testing | MockMvc / WebTestClient | HTTP request/response testing without starting server |
| Assertions | AssertJ | Fluent assertion library for readable test assertions |
| Frontend testing | Jasmine + Karma | Angular component and service unit tests |

## 9. Technical considerations

### 9.1 Architecture style

**Modular monolith** — a single deployable Spring Boot application with clear module boundaries. Modules communicate via direct service-layer calls within the same JVM. Each module owns its entities, repositories, services, controllers, and DTOs.

Modules:
| Module | Responsibility |
|--------|---------------|
| `auth` | User registration, login, JWT management, role management |
| `catalog` | Product CRUD, product queries |
| `inventory` | Stock tracking, reservation, adjustment |
| `orders` | Order lifecycle, line items, status transitions |
| `payments` | Payment processing, gateway abstraction, idempotency |
| `common` | Shared exceptions, base DTOs, global config |

**Inter-module dependencies** (service-layer calls):
- `orders` → `catalog` (validate products exist and are active, get prices)
- `orders` → `inventory` (check and decrement stock, restore on cancellation)
- `orders` → `auth` (resolve current user for order ownership)
- `payments` → `orders` (validate order status, update to PAID)

### 9.2 Database schema

#### 9.2.1 users

| Column | Type | Constraints |
|--------|------|------------|
| id | UUID | PK, generated |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| first_name | VARCHAR(100) | NOT NULL |
| last_name | VARCHAR(100) | NOT NULL |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

#### 9.2.2 products

| Column | Type | Constraints |
|--------|------|------------|
| id | UUID | PK, generated |
| name | VARCHAR(255) | NOT NULL |
| description | TEXT | |
| price | DECIMAL(12,2) | NOT NULL, CHECK > 0 |
| sku | VARCHAR(50) | NOT NULL, UNIQUE |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE |
| version | INTEGER | NOT NULL, DEFAULT 0 (optimistic lock) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

#### 9.2.3 inventory

| Column | Type | Constraints |
|--------|------|------------|
| id | UUID | PK, generated |
| product_id | UUID | NOT NULL, FK → products(id), UNIQUE |
| quantity | INTEGER | NOT NULL, DEFAULT 0, CHECK >= 0 |
| version | INTEGER | NOT NULL, DEFAULT 0 (optimistic lock) |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

#### 9.2.4 orders

| Column | Type | Constraints |
|--------|------|------------|
| id | UUID | PK, generated |
| user_id | UUID | NOT NULL, FK → users(id) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'CONFIRMED' |
| total_amount | DECIMAL(12,2) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Index: `idx_orders_user_id` on user_id
Index: `idx_orders_status` on status
Index: `idx_orders_created_at` on created_at

#### 9.2.5 order_items

| Column | Type | Constraints |
|--------|------|------------|
| id | UUID | PK, generated |
| order_id | UUID | NOT NULL, FK → orders(id) ON DELETE CASCADE |
| product_id | UUID | NOT NULL, FK → products(id) |
| quantity | INTEGER | NOT NULL, CHECK > 0 |
| unit_price | DECIMAL(12,2) | NOT NULL |
| subtotal | DECIMAL(12,2) | NOT NULL |

Index: `idx_order_items_order_id` on order_id

#### 9.2.6 payments

| Column | Type | Constraints |
|--------|------|------------|
| id | UUID | PK, generated |
| order_id | UUID | NOT NULL, FK → orders(id) |
| amount | DECIMAL(12,2) | NOT NULL |
| status | VARCHAR(20) | NOT NULL |
| idempotency_key | VARCHAR(255) | NOT NULL, UNIQUE |
| gateway_reference | VARCHAR(255) | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Index: `idx_payments_order_id` on order_id
Index: `idx_payments_idempotency_key` on idempotency_key (unique)

#### 9.2.7 refresh_tokens

| Column | Type | Constraints |
|--------|------|------------|
| id | UUID | PK, generated |
| user_id | UUID | NOT NULL, FK → users(id) ON DELETE CASCADE |
| token | VARCHAR(512) | NOT NULL, UNIQUE |
| expires_at | TIMESTAMP | NOT NULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

Index: `idx_refresh_tokens_token` on token (unique)

### 9.3 API specification

Base path: `/api/v1`

#### Auth endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /auth/register | Public | Register a new user |
| POST | /auth/login | Public | Login, returns tokens |
| POST | /auth/refresh | Public | Refresh access token |
| GET | /auth/me | USER, ADMIN | Get current user profile |

**POST /auth/register**
```json
// Request
{
  "email": "alex@example.com",
  "password": "securePassword123",
  "firstName": "Alex",
  "lastName": "Smith"
}
// Response 201
{
  "id": "uuid",
  "email": "alex@example.com",
  "firstName": "Alex",
  "lastName": "Smith",
  "role": "USER",
  "createdAt": "2026-02-07T10:00:00Z"
}
```

**POST /auth/login**
```json
// Request
{
  "email": "alex@example.com",
  "password": "securePassword123"
}
// Response 200
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "dGhpcyBpcyBh...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**POST /auth/refresh**
```json
// Request
{
  "refreshToken": "dGhpcyBpcyBh..."
}
// Response 200
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "bmV3IHJlZnJlc2g...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### Product endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /products | Public | List active products (paginated, filterable) |
| GET | /products/{id} | Public | Get product detail |
| POST | /admin/products | ADMIN | Create product |
| PUT | /admin/products/{id} | ADMIN | Update product |
| PATCH | /admin/products/{id}/status | ADMIN | Activate/deactivate product |

**GET /products?page=0&size=10&name=phone&minPrice=100&maxPrice=500&sort=price,asc**
```json
// Response 200 (Spring Page)
{
  "content": [
    {
      "id": "uuid",
      "name": "Smartphone X",
      "description": "Latest model",
      "price": 299.99,
      "sku": "PHONE-001",
      "active": true
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

**POST /admin/products**
```json
// Request
{
  "name": "Smartphone X",
  "description": "Latest model",
  "price": 299.99,
  "sku": "PHONE-001"
}
// Response 201
{
  "id": "uuid",
  "name": "Smartphone X",
  "description": "Latest model",
  "price": 299.99,
  "sku": "PHONE-001",
  "active": true,
  "createdAt": "2026-02-07T10:00:00Z"
}
```

#### Inventory endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /admin/inventory | ADMIN | List all inventory (paginated) |
| GET | /admin/inventory/{productId} | ADMIN | Get inventory for a product |
| PUT | /admin/inventory/{productId} | ADMIN | Set stock quantity |
| PATCH | /admin/inventory/{productId}/adjust | ADMIN | Adjust stock by delta (+/-) |

**PUT /admin/inventory/{productId}**
```json
// Request
{ "quantity": 100 }
// Response 200
{
  "productId": "uuid",
  "productName": "Smartphone X",
  "quantity": 100,
  "updatedAt": "2026-02-07T10:00:00Z"
}
```

**PATCH /admin/inventory/{productId}/adjust**
```json
// Request
{ "adjustment": -5 }
// Response 200
{
  "productId": "uuid",
  "productName": "Smartphone X",
  "quantity": 95,
  "updatedAt": "2026-02-07T10:00:00Z"
}
```

#### Order endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /orders | USER, ADMIN | Place a new order |
| GET | /orders | USER, ADMIN | List own orders (paginated, filterable) |
| GET | /orders/{id} | USER, ADMIN | Get order detail (own only) |
| POST | /orders/{id}/cancel | USER, ADMIN | Cancel own order |
| GET | /admin/orders | ADMIN | List all orders (paginated, filterable) |
| GET | /admin/orders/{id} | ADMIN | Get any order detail |
| POST | /admin/orders/{id}/cancel | ADMIN | Cancel any order |

**POST /orders**
```json
// Request
{
  "items": [
    { "productId": "uuid-1", "quantity": 2 },
    { "productId": "uuid-2", "quantity": 1 }
  ]
}
// Response 201
{
  "id": "uuid",
  "status": "CONFIRMED",
  "items": [
    {
      "productId": "uuid-1",
      "productName": "Smartphone X",
      "quantity": 2,
      "unitPrice": 299.99,
      "subtotal": 599.98
    },
    {
      "productId": "uuid-2",
      "productName": "Case Y",
      "quantity": 1,
      "unitPrice": 19.99,
      "subtotal": 19.99
    }
  ],
  "totalAmount": 619.97,
  "createdAt": "2026-02-07T10:00:00Z"
}
```

**GET /orders?page=0&size=10&status=CONFIRMED&createdAfter=2026-01-01&createdBefore=2026-12-31**

#### Payment endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /orders/{orderId}/payments | USER, ADMIN | Pay for an order |
| GET | /orders/{orderId}/payments | USER, ADMIN | Get payment status for an order |

**POST /orders/{orderId}/payments**
```
Header: Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```
```json
// Request
{
  "amount": 619.97
}
// Response 200 (success)
{
  "id": "uuid",
  "orderId": "uuid",
  "amount": 619.97,
  "status": "SUCCESS",
  "gatewayReference": "MOCK-REF-abc123",
  "createdAt": "2026-02-07T10:00:00Z"
}
// Response 200 (failure)
{
  "id": "uuid",
  "orderId": "uuid",
  "amount": 619.97,
  "status": "FAILED",
  "gatewayReference": null,
  "createdAt": "2026-02-07T10:00:00Z"
}
```

#### User management endpoints (admin)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /admin/users | ADMIN | List all users (paginated) |
| GET | /admin/users/{id} | ADMIN | Get user detail |
| PUT | /admin/users/{id}/role | ADMIN | Update user role |

**PUT /admin/users/{id}/role**
```json
// Request
{ "role": "ADMIN" }
// Response 200
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "Alex",
  "lastName": "Smith",
  "role": "ADMIN",
  "updatedAt": "2026-02-07T10:00:00Z"
}
```

#### Error responses (RFC 7807 Problem Details)

All error responses follow the RFC 7807 format:

```json
{
  "type": "https://orderhub.example.com/problems/insufficient-stock",
  "title": "Insufficient Stock",
  "status": 409,
  "detail": "Product 'Smartphone X' has only 3 units available but 5 were requested.",
  "instance": "/api/v1/orders"
}
```

Common error types:
| Type | Status | When |
|------|--------|------|
| validation-error | 400 | Request body validation fails |
| authentication-required | 401 | Missing or invalid JWT |
| access-denied | 403 | Insufficient role permissions |
| resource-not-found | 404 | Entity not found by ID |
| duplicate-resource | 409 | Duplicate email, SKU, or idempotency key |
| insufficient-stock | 409 | Not enough inventory |
| invalid-order-state | 409 | Invalid state transition (e.g., paying a cancelled order) |
| payment-amount-mismatch | 422 | Payment amount does not match order total |
| optimistic-lock-conflict | 409 | Concurrent modification detected |

### 9.4 Security architecture

**JWT flow:**
1. User registers or logs in → server returns access token (15 min) + refresh token (7 days)
2. Client stores tokens (access in memory, refresh in HttpOnly consideration or local storage for SPA)
3. Every API request includes `Authorization: Bearer <accessToken>` header
4. `JwtAuthenticationFilter` (OncePerRequestFilter) extracts token, validates signature and expiry, loads user details, sets SecurityContext
5. On 401 from expired access token, client calls `/auth/refresh` with refresh token
6. Server validates refresh token, issues new access + refresh tokens (rotation)
7. Old refresh token is invalidated

**Security filter chain configuration:**
```
Public:     POST /auth/register, /auth/login, /auth/refresh
            GET /products, /products/{id}
            GET /swagger-ui/**, /v3/api-docs/**
            GET /actuator/health

Authenticated (USER + ADMIN):
            GET /auth/me
            POST /orders
            GET /orders, /orders/{id}
            POST /orders/{id}/cancel
            POST /orders/{orderId}/payments
            GET /orders/{orderId}/payments

Admin only: /admin/**
```

### 9.5 Integration points

- **PostgreSQL**: Primary data store. Connected via Spring Data JPA with HikariCP connection pool
- **Flyway**: Database migration on application startup. Versioned SQL scripts under `src/main/resources/db/migration/`
- **Mock payment gateway**: Internal implementation. Strategy pattern allows swapping to Stripe/PayPal later by implementing `PaymentGateway` interface
- **Spring Boot Actuator**: Exposes /actuator/health, /actuator/info, /actuator/metrics endpoints

### 9.6 Data storage and privacy

- Passwords hashed with BCrypt (strength 10)
- No PII stored beyond email and name (both required for account)
- JWT tokens are stateless — no server-side session storage (except refresh tokens in DB)
- Refresh tokens stored in database for revocation capability
- No external data sharing

### 9.7 Scalability and performance

- Optimistic locking prevents inventory overselling without pessimistic locks
- Database indexes on foreign keys and frequently queried columns
- Pagination on all list endpoints to prevent unbounded queries
- HikariCP connection pooling with sensible defaults
- Stateless JWT authentication — no session affinity required
- Designed for single-instance deployment (MVP); architecture supports horizontal scaling

### 9.8 Potential challenges

- **Race condition on inventory**: Optimistic locking with @Version handles concurrent stock decrements. On conflict, `OptimisticLockException` is caught and translated to a user-friendly error
- **Token refresh race**: Multiple concurrent requests with expired token may race to refresh. Angular interceptor should queue requests and retry after single refresh
- **Cart-to-order price drift**: Prices may change between cart addition and order placement. Order captures snapshot prices. Frontend should display a warning if cart prices are stale
- **Flyway migration ordering**: Strict version numbering (V1__, V2__, etc.) ensures deterministic schema evolution

## 10. Project structure

```
order-system-mvp/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/orderhub/
│       │   │   ├── OrderHubApplication.java
│       │   │   ├── common/
│       │   │   │   ├── config/
│       │   │   │   │   ├── CorsConfig.java
│       │   │   │   │   └── OpenApiConfig.java
│       │   │   │   ├── dto/
│       │   │   │   │   └── PageResponse.java
│       │   │   │   └── exception/
│       │   │   │       ├── GlobalExceptionHandler.java
│       │   │   │       ├── ResourceNotFoundException.java
│       │   │   │       ├── DuplicateResourceException.java
│       │   │   │       ├── InsufficientStockException.java
│       │   │   │       ├── InvalidOrderStateException.java
│       │   │   │       └── PaymentAmountMismatchException.java
│       │   │   ├── auth/
│       │   │   │   ├── controller/
│       │   │   │   │   ├── AuthController.java
│       │   │   │   │   └── AdminUserController.java
│       │   │   │   ├── dto/
│       │   │   │   │   ├── RegisterRequest.java
│       │   │   │   │   ├── LoginRequest.java
│       │   │   │   │   ├── TokenResponse.java
│       │   │   │   │   ├── RefreshRequest.java
│       │   │   │   │   ├── UserResponse.java
│       │   │   │   │   └── UpdateRoleRequest.java
│       │   │   │   ├── entity/
│       │   │   │   │   ├── User.java
│       │   │   │   │   ├── Role.java (enum)
│       │   │   │   │   └── RefreshToken.java
│       │   │   │   ├── repository/
│       │   │   │   │   ├── UserRepository.java
│       │   │   │   │   └── RefreshTokenRepository.java
│       │   │   │   ├── service/
│       │   │   │   │   ├── AuthService.java
│       │   │   │   │   └── AuthServiceImpl.java
│       │   │   │   └── security/
│       │   │   │       ├── SecurityConfig.java
│       │   │   │       ├── JwtAuthenticationFilter.java
│       │   │   │       ├── JwtTokenProvider.java
│       │   │   │       ├── CustomUserDetailsService.java
│       │   │   │       └── JwtAuthEntryPoint.java
│       │   │   ├── catalog/
│       │   │   │   ├── controller/
│       │   │   │   │   ├── ProductController.java
│       │   │   │   │   └── AdminProductController.java
│       │   │   │   ├── dto/
│       │   │   │   │   ├── ProductRequest.java
│       │   │   │   │   ├── ProductResponse.java
│       │   │   │   │   └── ProductStatusRequest.java
│       │   │   │   ├── entity/
│       │   │   │   │   └── Product.java
│       │   │   │   ├── repository/
│       │   │   │   │   └── ProductRepository.java
│       │   │   │   └── service/
│       │   │   │       ├── ProductService.java
│       │   │   │       └── ProductServiceImpl.java
│       │   │   ├── inventory/
│       │   │   │   ├── controller/
│       │   │   │   │   └── AdminInventoryController.java
│       │   │   │   ├── dto/
│       │   │   │   │   ├── InventoryResponse.java
│       │   │   │   │   ├── SetStockRequest.java
│       │   │   │   │   └── AdjustStockRequest.java
│       │   │   │   ├── entity/
│       │   │   │   │   └── Inventory.java
│       │   │   │   ├── repository/
│       │   │   │   │   └── InventoryRepository.java
│       │   │   │   └── service/
│       │   │   │       ├── InventoryService.java
│       │   │   │       └── InventoryServiceImpl.java
│       │   │   ├── orders/
│       │   │   │   ├── controller/
│       │   │   │   │   ├── OrderController.java
│       │   │   │   │   └── AdminOrderController.java
│       │   │   │   ├── dto/
│       │   │   │   │   ├── CreateOrderRequest.java
│       │   │   │   │   ├── OrderItemRequest.java
│       │   │   │   │   ├── OrderResponse.java
│       │   │   │   │   └── OrderItemResponse.java
│       │   │   │   ├── entity/
│       │   │   │   │   ├── Order.java
│       │   │   │   │   ├── OrderItem.java
│       │   │   │   │   └── OrderStatus.java (enum)
│       │   │   │   ├── repository/
│       │   │   │   │   └── OrderRepository.java
│       │   │   │   └── service/
│       │   │   │       ├── OrderService.java
│       │   │   │       └── OrderServiceImpl.java
│       │   │   └── payments/
│       │   │       ├── controller/
│       │   │       │   └── PaymentController.java
│       │   │       ├── dto/
│       │   │       │   ├── PaymentRequest.java
│       │   │       │   └── PaymentResponse.java
│       │   │       ├── entity/
│       │   │       │   ├── Payment.java
│       │   │       │   └── PaymentStatus.java (enum)
│       │   │       ├── gateway/
│       │   │       │   ├── PaymentGateway.java (interface)
│       │   │       │   ├── PaymentGatewayResult.java
│       │   │       │   └── MockPaymentGateway.java
│       │   │       ├── repository/
│       │   │       │   └── PaymentRepository.java
│       │   │       └── service/
│       │   │           ├── PaymentService.java
│       │   │           └── PaymentServiceImpl.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── db/migration/
│       │           ├── V1__create_users_table.sql
│       │           ├── V2__create_products_table.sql
│       │           ├── V3__create_inventory_table.sql
│       │           ├── V4__create_orders_tables.sql
│       │           ├── V5__create_payments_table.sql
│       │           ├── V6__create_refresh_tokens_table.sql
│       │           └── V7__seed_dev_data.sql
│       └── test/
│           └── java/com/orderhub/
│               ├── auth/
│               │   ├── service/AuthServiceTest.java
│               │   └── controller/AuthControllerIntegrationTest.java
│               ├── catalog/
│               │   ├── service/ProductServiceTest.java
│               │   └── controller/ProductControllerIntegrationTest.java
│               ├── inventory/
│               │   ├── service/InventoryServiceTest.java
│               │   └── repository/InventoryRepositoryTest.java
│               ├── orders/
│               │   ├── service/OrderServiceTest.java
│               │   └── controller/OrderControllerIntegrationTest.java
│               └── payments/
│                   ├── service/PaymentServiceTest.java
│                   └── controller/PaymentControllerIntegrationTest.java
├── frontend/
│   ├── angular.json
│   ├── package.json
│   ├── tsconfig.json
│   └── src/
│       ├── app/
│       │   ├── app.component.ts
│       │   ├── app.config.ts
│       │   ├── app.routes.ts
│       │   ├── core/
│       │   │   ├── guards/
│       │   │   │   ├── auth.guard.ts
│       │   │   │   └── admin.guard.ts
│       │   │   ├── interceptors/
│       │   │   │   └── auth.interceptor.ts
│       │   │   ├── services/
│       │   │   │   ├── auth.service.ts
│       │   │   │   ├── product.service.ts
│       │   │   │   ├── order.service.ts
│       │   │   │   ├── payment.service.ts
│       │   │   │   ├── cart.service.ts
│       │   │   │   └── admin.service.ts
│       │   │   └── models/
│       │   │       ├── user.model.ts
│       │   │       ├── product.model.ts
│       │   │       ├── order.model.ts
│       │   │       ├── payment.model.ts
│       │   │       └── cart.model.ts
│       │   ├── shared/
│       │   │   ├── components/
│       │   │   │   ├── navbar/
│       │   │   │   ├── pagination/
│       │   │   │   ├── toast/
│       │   │   │   └── confirm-dialog/
│       │   │   └── pipes/
│       │   │       └── currency-format.pipe.ts
│       │   └── features/
│       │       ├── auth/
│       │       │   ├── login/
│       │       │   │   └── login.component.ts
│       │       │   └── register/
│       │       │       └── register.component.ts
│       │       ├── catalog/
│       │       │   ├── product-list/
│       │       │   │   └── product-list.component.ts
│       │       │   └── product-detail/
│       │       │       └── product-detail.component.ts
│       │       ├── cart/
│       │       │   └── cart.component.ts
│       │       ├── checkout/
│       │       │   └── checkout.component.ts
│       │       ├── orders/
│       │       │   ├── order-list/
│       │       │   │   └── order-list.component.ts
│       │       │   └── order-detail/
│       │       │       └── order-detail.component.ts
│       │       └── admin/
│       │           ├── dashboard/
│       │           │   └── admin-dashboard.component.ts
│       │           ├── products/
│       │           │   ├── product-management.component.ts
│       │           │   └── product-form.component.ts
│       │           ├── inventory/
│       │           │   └── inventory-management.component.ts
│       │           ├── users/
│       │           │   └── user-management.component.ts
│       │           └── orders/
│       │               └── order-management.component.ts
│       ├── environments/
│       │   ├── environment.ts
│       │   └── environment.development.ts
│       └── styles.css
├── docker-compose.yml
├── .gitignore
└── docs/
    ├── OrderHub_MVP.md
    └── prd.md
```

## 11. Frontend architecture

### 11.1 State management

- **Angular Signals** for reactive state management
- **Cart state**: Maintained in `CartService` using signals. Cart is client-side only (not persisted to server). Stored in localStorage for persistence across browser sessions
- **Auth state**: `AuthService` manages current user, tokens, and authentication status via signals
- **No NgRx or external state library** — signals are sufficient for MVP scope

### 11.2 Routing

```typescript
// app.routes.ts (simplified)
export const routes: Routes = [
  { path: 'login', loadComponent: () => LoginComponent },
  { path: 'register', loadComponent: () => RegisterComponent },
  { path: 'products', loadComponent: () => ProductListComponent },
  { path: 'products/:id', loadComponent: () => ProductDetailComponent },
  { path: 'cart', loadComponent: () => CartComponent, canActivate: [authGuard] },
  { path: 'checkout', loadComponent: () => CheckoutComponent, canActivate: [authGuard] },
  { path: 'orders', loadComponent: () => OrderListComponent, canActivate: [authGuard] },
  { path: 'orders/:id', loadComponent: () => OrderDetailComponent, canActivate: [authGuard] },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    children: [
      { path: '', loadComponent: () => AdminDashboardComponent },
      { path: 'products', loadComponent: () => ProductManagementComponent },
      { path: 'inventory', loadComponent: () => InventoryManagementComponent },
      { path: 'users', loadComponent: () => UserManagementComponent },
      { path: 'orders', loadComponent: () => OrderManagementComponent },
    ]
  },
  { path: '', redirectTo: 'products', pathMatch: 'full' },
];
```

### 11.3 HTTP interceptor

The `AuthInterceptor` attaches the JWT access token to all API requests and handles token refresh on 401 responses:
- On outgoing request: add `Authorization: Bearer <token>` header if user is authenticated
- On 401 response: attempt token refresh, queue concurrent requests, retry original request with new token
- On refresh failure: clear auth state and redirect to login

### 11.4 CORS configuration

Backend `CorsConfig` allows requests from `http://localhost:4200` (Angular dev server) with credentials support.

## 12. Docker Compose

```yaml
services:
  app:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/orderhub
      SPRING_DATASOURCE_USERNAME: orderhub
      SPRING_DATASOURCE_PASSWORD: orderhub
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: orderhub
      POSTGRES_USER: orderhub
      POSTGRES_PASSWORD: orderhub
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U orderhub"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

## 13. Seed data (V7__seed_dev_data.sql)

- **Admin user**: admin@orderhub.com / admin123 (BCrypt hashed), role=ADMIN
- **Test user**: user@orderhub.com / user123 (BCrypt hashed), role=USER
- **Sample products** (5-10 items):
  - Smartphone X (SKU: PHONE-001, $299.99)
  - Laptop Pro (SKU: LAPTOP-001, $999.99)
  - Wireless Earbuds (SKU: AUDIO-001, $79.99)
  - USB-C Cable (SKU: CABLE-001, $12.99)
  - Phone Case (SKU: CASE-001, $19.99)
- **Inventory**: Each product seeded with 50-100 units

## 14. Testing strategy

### 14.1 Unit tests (JUnit 5 + Mockito)

Test each service class in isolation:
- `AuthServiceTest`: registration validation, login, token generation, role update
- `ProductServiceTest`: CRUD operations, filter/pagination logic, SKU uniqueness
- `InventoryServiceTest`: stock adjustment, decrement, restore, optimistic lock handling
- `OrderServiceTest`: order creation atomicity, status transitions, cancellation with inventory restore
- `PaymentServiceTest`: payment processing, idempotency, amount validation, status transitions

### 14.2 Integration tests (@SpringBootTest + Testcontainers)

Test the full request-response cycle with a real PostgreSQL instance:
- `AuthControllerIntegrationTest`: register, login, refresh, protected endpoint access
- `ProductControllerIntegrationTest`: CRUD via HTTP, pagination, filters, admin-only access
- `OrderControllerIntegrationTest`: full order lifecycle (create → pay or cancel), inventory effects
- `PaymentControllerIntegrationTest`: payment with idempotency, amount mismatch, invalid order state
- `InventoryRepositoryTest`: optimistic locking under concurrent updates

### 14.3 Frontend tests (Jasmine/Karma)

- Component unit tests for key components (login form, product list, cart, checkout)
- Service tests for HTTP call correctness (using HttpClientTestingModule)
- Guard tests for route protection logic

## 15. User stories

### US-001: User registration

- **ID**: OH-001
- **Description**: As a new user, I want to register an account so that I can place orders.
- **Acceptance criteria**:
  - User provides email, password, first name, and last name
  - Email must be unique; duplicate returns 409 error
  - Password is stored as BCrypt hash
  - User is assigned the USER role by default
  - Response returns user details without password
  - Invalid input returns 400 with validation details

### US-002: User login

- **ID**: OH-002
- **Description**: As a registered user, I want to log in so that I can access authenticated features.
- **Acceptance criteria**:
  - User provides email and password
  - Correct credentials return access token (15 min) and refresh token (7 days)
  - Incorrect credentials return 401 error
  - Response includes token type and expiry duration

### US-003: Token refresh

- **ID**: OH-003
- **Description**: As an authenticated user, I want to refresh my access token so that I stay logged in without re-entering credentials.
- **Acceptance criteria**:
  - Valid refresh token returns new access + refresh token pair
  - Old refresh token is invalidated (rotation)
  - Expired or invalid refresh token returns 401
  - Concurrent refresh with same token is handled gracefully

### US-004: View own profile

- **ID**: OH-004
- **Description**: As an authenticated user, I want to view my profile information.
- **Acceptance criteria**:
  - GET /auth/me returns current user's id, email, name, and role
  - Unauthenticated request returns 401

### US-005: Browse products

- **ID**: OH-005
- **Description**: As a customer, I want to browse available products so that I can find items to purchase.
- **Acceptance criteria**:
  - Product listing is publicly accessible (no auth required)
  - Only active products are returned
  - Results are paginated (default page=0, size=20)
  - Products can be filtered by name (case-insensitive partial match)
  - Products can be filtered by price range (minPrice, maxPrice)
  - Results can be sorted by name or price

### US-006: View product detail

- **ID**: OH-006
- **Description**: As a customer, I want to view detailed information about a product.
- **Acceptance criteria**:
  - Product detail is publicly accessible
  - Returns product name, description, price, and SKU
  - Non-existent product ID returns 404

### US-007: Add to cart

- **ID**: OH-007
- **Description**: As a customer, I want to add products to my cart so that I can purchase multiple items at once.
- **Acceptance criteria**:
  - Cart is managed client-side in Angular (no server API)
  - Adding a product already in cart increases its quantity
  - Cart persists across page refreshes (localStorage)
  - Cart displays item count in the navigation bar

### US-008: Manage cart

- **ID**: OH-008
- **Description**: As a customer, I want to view and modify my cart before checkout.
- **Acceptance criteria**:
  - Cart page displays all items with product name, unit price, quantity, and subtotal
  - User can increase/decrease quantity for each item
  - User can remove items from cart
  - Cart total is displayed and updates in real-time
  - Empty cart shows a message with link to products

### US-009: Place order (checkout)

- **ID**: OH-009
- **Description**: As a customer, I want to place an order for the items in my cart.
- **Acceptance criteria**:
  - User must be authenticated to place an order
  - System validates all products exist and are active
  - System checks and decrements inventory atomically for all items
  - If any item has insufficient stock, entire order is rejected with details of which items failed
  - Order is created with status CONFIRMED and captures unit prices at order time
  - Order total equals sum of (unit_price * quantity) for all items
  - Cart is cleared on successful order placement
  - Response includes order ID and full order details

### US-010: View order history

- **ID**: OH-010
- **Description**: As a customer, I want to view my past orders so that I can track their status.
- **Acceptance criteria**:
  - Returns only the authenticated user's orders
  - Results are paginated
  - Can filter by order status (CONFIRMED, PAID, CANCELLED)
  - Can filter by date range
  - Each order shows status, total, item count, and creation date

### US-011: View order detail

- **ID**: OH-011
- **Description**: As a customer, I want to view the full details of a specific order.
- **Acceptance criteria**:
  - Returns order with all line items, quantities, prices, and subtotals
  - Shows order status and total amount
  - Shows payment information if payment has been attempted
  - User can only view their own orders; others return 404
  - Non-existent order returns 404

### US-012: Cancel order

- **ID**: OH-012
- **Description**: As a customer, I want to cancel my unpaid order so that inventory is released.
- **Acceptance criteria**:
  - Only orders with status CONFIRMED can be cancelled
  - Cancelling a PAID order returns 409 error
  - Cancellation restores inventory for all line items
  - Order status changes to CANCELLED
  - User can only cancel their own orders

### US-013: Pay for order

- **ID**: OH-013
- **Description**: As a customer, I want to pay for my confirmed order.
- **Acceptance criteria**:
  - Requires Idempotency-Key header (UUID format)
  - Payment amount must exactly equal the order total; mismatch returns 422
  - Only CONFIRMED orders can be paid; wrong status returns 409
  - Successful payment changes order status to PAID
  - Failed payment leaves order in CONFIRMED state (retryable)
  - Duplicate Idempotency-Key returns the original payment result without reprocessing
  - Payment response includes status (SUCCESS/FAILED) and gateway reference

### US-014: Admin create product

- **ID**: OH-014
- **Description**: As an admin, I want to create new products in the catalog.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Product requires name, price (positive), and SKU (unique)
  - Duplicate SKU returns 409 error
  - New product is active by default
  - Inventory record is automatically created with quantity 0

### US-015: Admin update product

- **ID**: OH-015
- **Description**: As an admin, I want to update existing product information.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Can update name, description, price, and SKU
  - Optimistic locking prevents lost updates from concurrent edits
  - Non-existent product returns 404

### US-016: Admin activate/deactivate product

- **ID**: OH-016
- **Description**: As an admin, I want to activate or deactivate products without deleting them.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Deactivated products are excluded from customer product listings
  - Deactivated products cannot be ordered (validation at order time)
  - Existing orders referencing deactivated products are not affected

### US-017: Admin set stock

- **ID**: OH-017
- **Description**: As an admin, I want to set the stock quantity for a product.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Sets absolute quantity value
  - Quantity must be >= 0
  - Returns updated inventory with product name and timestamp
  - Optimistic locking prevents concurrent update conflicts

### US-018: Admin adjust stock

- **ID**: OH-018
- **Description**: As an admin, I want to adjust stock by adding or subtracting units.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Accepts positive (add) or negative (subtract) adjustment value
  - Resulting quantity cannot go below zero; returns 409 if adjustment would cause negative stock
  - Returns updated inventory with new quantity

### US-019: Admin view all orders

- **ID**: OH-019
- **Description**: As an admin, I want to view all orders across all users.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Returns all orders with pagination
  - Can filter by status and date range
  - Each order includes user email, status, total, and creation date

### US-020: Admin cancel any order

- **ID**: OH-020
- **Description**: As an admin, I want to cancel any unpaid order.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Can cancel any user's CONFIRMED order
  - PAID orders cannot be cancelled (returns 409)
  - Cancellation restores inventory for all line items

### US-021: Admin list users

- **ID**: OH-021
- **Description**: As an admin, I want to view all registered users.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Returns paginated list of users with id, email, name, role, and registration date
  - Does not expose password hashes

### US-022: Admin promote user

- **ID**: OH-022
- **Description**: As an admin, I want to promote a user to admin role.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Changes user's role from USER to ADMIN
  - Already-ADMIN user returns success (idempotent)
  - Non-existent user returns 404

### US-023: View inventory (admin)

- **ID**: OH-023
- **Description**: As an admin, I want to view inventory levels for all products.
- **Acceptance criteria**:
  - Only ADMIN role can access this endpoint
  - Returns paginated list of products with their stock quantities
  - Includes product name, SKU, and current quantity

### US-024: Concurrent order handling

- **ID**: OH-024
- **Description**: As the system, I want to handle concurrent orders for the same product without overselling.
- **Acceptance criteria**:
  - Two simultaneous orders for the last unit: only one succeeds, the other gets 409 error
  - Optimistic locking exception is caught and returned as a user-friendly error message
  - No inventory goes negative under any concurrency scenario

### US-025: Payment idempotency

- **ID**: OH-025
- **Description**: As the system, I want to ensure duplicate payment requests do not result in double charges.
- **Acceptance criteria**:
  - Same Idempotency-Key for same order returns the original payment result
  - Different Idempotency-Key for same order processes a new payment attempt
  - Missing Idempotency-Key header returns 400 error

### US-026: API documentation

- **ID**: OH-026
- **Description**: As a developer, I want to access auto-generated API documentation via Swagger UI.
- **Acceptance criteria**:
  - Swagger UI is accessible at /swagger-ui.html (no authentication required)
  - All endpoints are documented with request/response schemas
  - Endpoints are grouped by module/tag (Auth, Products, Inventory, Orders, Payments, Admin)
  - Example request/response bodies are included

### US-027: Health check

- **ID**: OH-027
- **Description**: As an operator, I want to check the application's health status.
- **Acceptance criteria**:
  - GET /actuator/health returns application status (UP/DOWN)
  - Includes database connectivity check
  - Accessible without authentication
