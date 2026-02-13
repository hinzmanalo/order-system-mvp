# OrderHub Backend - Architecture & System Design

## Table of Contents

1. [Overview](#overview)
2. [Architecture Style](#architecture-style)
3. [Technology Stack](#technology-stack)
4. [Module Structure](#module-structure)
5. [Domain Model](#domain-model)
6. [Data Flow & Sequence Diagrams](#data-flow--sequence-diagrams)
7. [API Design](#api-design)
8. [Security Architecture](#security-architecture)
9. [Database Design](#database-design)
10. [Concurrency & Transaction Management](#concurrency--transaction-management)
11. [Error Handling](#error-handling)
12. [Configuration Management](#configuration-management)
13. [Testing Strategy](#testing-strategy)
14. [Deployment Architecture](#deployment-architecture)

---

## Overview

OrderHub is a **portfolio-grade monolithic ordering system** demonstrating enterprise-level backend engineering practices. The backend is built with Spring Boot 3 and follows a **modular monolith architecture** pattern.

### Key Characteristics

- **Single Deployable Unit**: All modules are packaged and deployed together
- **Clean Domain Boundaries**: Each module owns its domain logic and data
- **Transaction Integrity**: Atomic operations across inventory and orders
- **Stateless Authentication**: JWT-based security with no server-side sessions
- **API-First Design**: RESTful endpoints with OpenAPI documentation

---

## Architecture Style

### Modular Monolith Pattern

```
┌─────────────────────────────────────────────────────────────────┐
│                      OrderHub Application                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌───────────┐  ┌────────┐  ┌───────┐│
│  │  Auth   │  │ Catalog │  │ Inventory │  │ Orders │  │Common ││
│  └────┬────┘  └────┬────┘  └─────┬─────┘  └───┬────┘  └───┬───┘│
│       │            │             │            │           │     │
│       └────────────┴─────────────┴────────────┴───────────┘     │
│                              │                                   │
│                    Spring Framework Core                         │
├─────────────────────────────────────────────────────────────────┤
│                         PostgreSQL                               │
└─────────────────────────────────────────────────────────────────┘
```

### Module Dependency Graph

```
common ← auth ← catalog ← inventory ← orders
   ↑       ↑        ↑          ↑         ↑
   └───────┴────────┴──────────┴─────────┘
   (shared exceptions, config, utilities)
```

**Dependency Rules:**

- Modules can only depend on modules to their left
- `common` has no module dependencies (foundational)
- `orders` can access all other modules
- Inter-module calls use **direct service injection** (same JVM)

---

## Technology Stack

| Layer      | Technology      | Version  | Purpose                        |
| ---------- | --------------- | -------- | ------------------------------ |
| Runtime    | Java            | 17 (LTS) | Application runtime            |
| Framework  | Spring Boot     | 3.x      | Application framework          |
| Web        | Spring MVC      | 6.x      | REST API layer                 |
| Security   | Spring Security | 6.x      | Authentication & authorization |
| Data       | Spring Data JPA | 3.x      | ORM and repository pattern     |
| Database   | PostgreSQL      | 16       | Primary data store             |
| Migrations | Flyway          | 9.x      | Schema versioning              |
| Build      | Maven           | 3.x      | Dependency management          |
| Containers | Docker          | -        | Containerization               |
| API Docs   | OpenAPI/Swagger | 3.0      | API documentation              |

---

## Module Structure

Each business module follows a consistent layered structure:

```
com.orderhub.{module}/
├── controller/     # REST API endpoints (@RestController)
├── dto/            # Data Transfer Objects (Request/Response)
├── entity/         # JPA entities (domain model)
├── repository/     # Spring Data JPA interfaces
└── service/        # Business logic (Interface + Impl)
```

### Module Breakdown

#### Common Module (`com.orderhub.common`)

Shared infrastructure components used across all modules.

```
common/
├── config/
│   ├── CorsConfig.java          # CORS configuration
│   └── OpenApiConfig.java       # Swagger/OpenAPI setup
└── exception/
    ├── GlobalExceptionHandler.java    # Centralized error handling
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    ├── ConflictException.java
    ├── UnauthorizedException.java
    ├── InsufficientStockException.java
    ├── InvalidOrderStateException.java
    └── PaymentAmountMismatchException.java
```

#### Auth Module (`com.orderhub.auth`)

User authentication, authorization, and session management.

```
auth/
├── controller/
│   └── AuthController.java      # Login, register, refresh endpoints
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   └── UserResponse.java
├── entity/
│   ├── User.java               # User entity
│   ├── Role.java               # USER, ADMIN enum
│   └── RefreshToken.java       # Token persistence
├── repository/
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
├── security/
│   ├── SecurityConfig.java         # Spring Security configuration
│   ├── JwtTokenProvider.java       # JWT creation/validation
│   ├── JwtAuthenticationFilter.java # Request filter
│   ├── JwtAuthEntryPoint.java      # Auth error handling
│   └── CustomUserDetailsService.java
└── service/
    ├── AuthService.java
    └── AuthServiceImpl.java
```

#### Catalog Module (`com.orderhub.catalog`)

Product management and browsing.

```
catalog/
├── controller/
│   └── ProductController.java   # CRUD + search endpoints
├── dto/
│   ├── CreateProductRequest.java
│   ├── UpdateProductRequest.java
│   └── ProductResponse.java
├── entity/
│   └── Product.java            # Product with @Version
├── repository/
│   └── ProductRepository.java  # Custom query methods
└── service/
    ├── ProductService.java
    └── ProductServiceImpl.java
```

#### Inventory Module (`com.orderhub.inventory`)

Stock level management with concurrency control.

```
inventory/
├── controller/
│   └── InventoryController.java  # Admin stock operations
├── dto/
│   ├── InventoryResponse.java
│   ├── SetStockRequest.java
│   └── AdjustStockRequest.java
├── entity/
│   └── Inventory.java           # Stock with @Version
├── repository/
│   └── InventoryRepository.java
└── service/
    ├── InventoryService.java
    └── InventoryServiceImpl.java
```

#### Orders Module (`com.orderhub.orders`)

Order lifecycle and management.

```
orders/
├── controller/
│   └── OrderController.java      # Order CRUD
├── dto/
│   ├── CreateOrderRequest.java
│   ├── OrderItemRequest.java
│   ├── OrderResponse.java
│   └── OrderItemResponse.java
├── entity/
│   ├── Order.java
│   ├── OrderItem.java
│   └── OrderStatus.java         # CONFIRMED, PAID, CANCELLED
├── repository/
│   └── OrderRepository.java
└── service/
    ├── OrderService.java
    └── OrderServiceImpl.java
```

---

## Domain Model

### Entity Relationship Diagram

```
┌──────────────────┐
│      User        │
├──────────────────┤
│ id: UUID (PK)    │
│ email: String    │
│ passwordHash     │
│ firstName        │
│ lastName         │
│ role: Role       │
│ createdAt        │
│ updatedAt        │
└────────┬─────────┘
         │ 1
         │
         │ *
┌────────┴─────────┐        ┌──────────────────┐
│      Order       │        │     Product      │
├──────────────────┤        ├──────────────────┤
│ id: UUID (PK)    │        │ id: UUID (PK)    │
│ user_id: FK      │        │ name: String     │
│ status: Enum     │        │ description      │
│ totalAmount      │        │ price: BigDecimal│
│ createdAt        │        │ sku: String (UQ) │
│ updatedAt        │        │ active: boolean  │
└────────┬─────────┘        │ version: int     │
         │ 1                │ createdAt        │
         │                  │ updatedAt        │
         │ *                └────────┬─────────┘
┌────────┴─────────┐                 │ 1
│    OrderItem     │                 │
├──────────────────┤                 │ 1
│ id: UUID (PK)    │        ┌────────┴─────────┐
│ order_id: FK     │        │    Inventory     │
│ product_id: FK   │        ├──────────────────┤
│ quantity: int    │        │ id: UUID (PK)    │
│ unitPrice        │        │ product_id: FK   │
│ subtotal         │        │ quantity: int    │
└──────────────────┘        │ version: int     │
                            │ updatedAt        │
                            └──────────────────┘
```

### Order State Machine

```
          ┌──────────────┐
          │   CREATED    │  (Internal only, not exposed)
          └──────┬───────┘
                 │ validate & decrement inventory
                 ▼
          ┌──────────────┐
          │  CONFIRMED   │
          └──────┬───────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
┌──────────────┐  ┌──────────────┐
│     PAID     │  │  CANCELLED   │
│   (final)    │  │   (final)    │
└──────────────┘  └──────────────┘
        │                 │
        └────────┬────────┘
                 │
                 ▼
         inventory restored
         (on cancellation)
```

---

## Data Flow & Sequence Diagrams

### Order Creation Flow

```
┌──────┐     ┌────────────┐     ┌─────────────┐     ┌───────────────┐     ┌─────────────────┐
│Client│     │OrderCtrl   │     │OrderService │     │InventoryService│    │OrderRepository  │
└──┬───┘     └─────┬──────┘     └──────┬──────┘     └───────┬───────┘     └────────┬────────┘
   │               │                   │                    │                      │
   │ POST /orders  │                   │                    │                      │
   │──────────────>│                   │                    │                      │
   │               │ createOrder()     │                    │                      │
   │               │──────────────────>│                    │                      │
   │               │                   │                    │                      │
   │               │                   │ BEGIN TRANSACTION  │                      │
   │               │                   │═══════════════════════════════════════════│
   │               │                   │                    │                      │
   │               │                   │ for each item:     │                      │
   │               │                   │ decrementStock()   │                      │
   │               │                   │───────────────────>│                      │
   │               │                   │                    │ check stock          │
   │               │                   │                    │ update with version  │
   │               │                   │<───────────────────│                      │
   │               │                   │                    │                      │
   │               │                   │ save(order)        │                      │
   │               │                   │────────────────────────────────────────── │
   │               │                   │                    │                      │
   │               │                   │ COMMIT TRANSACTION │                      │
   │               │                   │═══════════════════════════════════════════│
   │               │                   │                    │                      │
   │               │<──────────────────│                    │                      │
   │<──────────────│ 201 Created       │                    │                      │
   │               │                   │                    │                      │
```

### Authentication Flow

```
┌──────┐     ┌────────────┐     ┌─────────────┐     ┌─────────────────┐
│Client│     │AuthCtrl    │     │AuthService  │     │JwtTokenProvider │
└──┬───┘     └─────┬──────┘     └──────┬──────┘     └────────┬────────┘
   │               │                   │                     │
   │ POST /login   │                   │                     │
   │──────────────>│                   │                     │
   │               │ login()           │                     │
   │               │──────────────────>│                     │
   │               │                   │ authenticate user   │
   │               │                   │ (BCrypt verify)     │
   │               │                   │                     │
   │               │                   │ generateToken()     │
   │               │                   │────────────────────>│
   │               │                   │<────────────────────│
   │               │                   │ (access + refresh)  │
   │               │<──────────────────│                     │
   │<──────────────│ 200 OK (tokens)   │                     │
   │               │                   │                     │
   │               │                   │                     │
   │ GET /orders   │                   │                     │
   │ Authorization:│                   │                     │
   │ Bearer <jwt>  │                   │                     │
   │──────────────>│                   │                     │
   │               │                   │                     │
   │               │ JwtAuthFilter intercepts               │
   │               │────────────────────────────────────────>│
   │               │                   │    validateToken()  │
   │               │<────────────────────────────────────────│
   │               │                   │                     │
   │               │ proceed to controller                  │
   │<──────────────│ 200 OK            │                     │
```

---

## API Design

### RESTful Conventions

| Operation   | HTTP Method | Path Pattern              | Response           |
| ----------- | ----------- | ------------------------- | ------------------ |
| Create      | POST        | `/api/v1/{resource}`      | 201 Created        |
| Read (one)  | GET         | `/api/v1/{resource}/{id}` | 200 OK             |
| Read (list) | GET         | `/api/v1/{resource}`      | 200 OK (paginated) |
| Update      | PUT         | `/api/v1/{resource}/{id}` | 200 OK             |
| Delete      | DELETE      | `/api/v1/{resource}/{id}` | 204 No Content     |

### API Endpoint Summary

```
Authentication
├── POST   /api/v1/auth/register     # User registration
├── POST   /api/v1/auth/login        # User login
├── POST   /api/v1/auth/refresh      # Token refresh
└── GET    /api/v1/auth/me           # Current user profile

Products (Public)
├── GET    /api/v1/products          # List products (paginated)
└── GET    /api/v1/products/{id}     # Get product details

Admin Products
├── POST   /api/v1/admin/products            # Create product
├── PUT    /api/v1/admin/products/{id}       # Update product
└── DELETE /api/v1/admin/products/{id}       # Deactivate product

Admin Inventory
├── GET    /api/v1/admin/inventory           # List all inventory
├── GET    /api/v1/admin/inventory/{productId}
├── PUT    /api/v1/admin/inventory/{productId}/set
└── PUT    /api/v1/admin/inventory/{productId}/adjust

Orders (Authenticated)
├── POST   /api/v1/orders            # Create order
├── GET    /api/v1/orders            # List user's orders
├── GET    /api/v1/orders/{id}       # Get order details
└── POST   /api/v1/orders/{id}/cancel # Cancel order

Payments (Authenticated)
└── POST   /api/v1/payments          # Process payment (with Idempotency-Key)

Admin Orders
└── GET    /api/v1/admin/orders      # List all orders
```

### Response Patterns

**Successful Response:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Product Name",
  "price": 29.99,
  "createdAt": "2026-02-13T10:30:00Z"
}
```

**Paginated Response:**

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true }
  },
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

**Error Response (RFC 7807):**

```json
{
  "type": "https://orderhub.example.com/problems/insufficient-stock",
  "title": "Insufficient Stock",
  "status": 409,
  "detail": "Not enough stock for product: Widget",
  "instance": "/api/v1/orders",
  "productName": "Widget",
  "available": 5,
  "requested": 10
}
```

---

## Security Architecture

### Authentication Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Security Filter Chain                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Request → JwtAuthenticationFilter → SecurityContext → Controller│
│                  │                                               │
│                  ▼                                               │
│         ┌───────────────────┐                                   │
│         │ JwtTokenProvider  │                                   │
│         ├───────────────────┤                                   │
│         │ - Parse JWT       │                                   │
│         │ - Validate sig    │                                   │
│         │ - Extract claims  │                                   │
│         │ - Check expiry    │                                   │
│         └───────────────────┘                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### JWT Token Structure

**Access Token (15 min validity):**

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1707820200,
  "exp": 1707821100
}
```

**Refresh Token (7 days validity):**

- Stored in database (`refresh_tokens` table)
- One-time use (deleted after refresh)
- Enables token rotation

### Role-Based Access Control

| Endpoint Pattern      | Required Role |
| --------------------- | ------------- |
| `/api/v1/auth/**`     | PUBLIC        |
| `/api/v1/products/**` | PUBLIC        |
| `/api/v1/admin/**`    | ADMIN         |
| `/api/v1/orders/**`   | USER or ADMIN |
| `/api/v1/payments/**` | USER or ADMIN |

### Security Configuration

```java
@EnableMethodSecurity
public class SecurityConfig {

    // Stateless session (no cookies/JSESSIONID)
    .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

    // CSRF disabled (stateless JWT)
    .csrf(csrf -> csrf.disable())

    // Authorization rules
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers("/api/v1/products/**").permitAll()
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/v1/**").authenticated())
}
```

---

## Database Design

### Schema Overview

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Products table (with optimistic locking)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Inventory table (with optimistic locking)
CREATE TABLE inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID UNIQUE NOT NULL REFERENCES products(id),
    quantity INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    total_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Order items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL
);

-- Payments table (with idempotency)
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    gateway_reference VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Indexing Strategy

```sql
-- Query optimization indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE UNIQUE INDEX idx_payments_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### Migration Strategy

Flyway manages schema evolution via numbered migration files:

```
db/migration/
├── V1__create_users_table.sql
├── V2__create_products_table.sql
├── V3__create_inventory_table.sql
├── V4__create_orders_tables.sql
├── V5__create_payments_table.sql
├── V6__create_refresh_tokens_table.sql
└── V7__seed_dev_data.sql
```

**Migration Rules:**

- Files are immutable after deployment
- Version numbers must be sequential
- `ddl-auto: validate` ensures schema matches entities

---

## Concurrency & Transaction Management

### Optimistic Locking Pattern

The system uses JPA `@Version` to prevent lost updates and overselling:

```java
@Entity
public class Inventory {
    @Version
    private int version;  // Auto-incremented on save

    private int quantity;
}
```

**Conflict Resolution:**

```
Thread A reads Inventory (version=1, quantity=10)
Thread B reads Inventory (version=1, quantity=10)
Thread A: quantity=5, saves → version=2 ✓
Thread B: quantity=7, saves → ObjectOptimisticLockingFailureException
           └→ Returns 409 Conflict to client
```

### Transaction Boundaries

```java
@Service
public class OrderServiceImpl {

    @Transactional  // All or nothing
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        // 1. Load user
        // 2. For each item:
        //    - Load product
        //    - Validate active
        //    - Decrement inventory (throws if insufficient)
        //    - Create order item
        // 3. Save order
        //
        // If ANY step fails → entire transaction rolls back
        // Inventory changes are reverted automatically
    }
}
```

### Atomic Order Creation

```
┌─────────────────────────────────────────────────────────┐
│                    TRANSACTION SCOPE                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │ For each order item:                             │    │
│  │   1. SELECT inventory WHERE product_id = ?       │    │
│  │   2. Check quantity >= requested                 │    │
│  │   3. UPDATE inventory SET quantity = quantity-?  │    │
│  │      WHERE id = ? AND version = ?               │    │
│  │   4. CREATE order_item                          │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │ INSERT INTO orders (...)                         │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  COMMIT (or ROLLBACK on any failure)                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Idempotency for Payments

```java
// Payment request with idempotency key
POST /api/v1/payments
Headers: Idempotency-Key: abc123

// First call: processes payment, stores key
// Subsequent calls with same key: returns cached result
// Prevents duplicate payments on network retries
```

---

## Error Handling

### Exception Hierarchy

```
RuntimeException
└── OrderHub Exceptions
    ├── ResourceNotFoundException (404)
    ├── DuplicateResourceException (409)
    ├── ConflictException (409)
    ├── UnauthorizedException (401)
    ├── InsufficientStockException (409)
    ├── InvalidOrderStateException (409)
    └── PaymentAmountMismatchException (422)
```

### Global Exception Handler

All exceptions are caught by `GlobalExceptionHandler` and converted to RFC 7807 Problem Detail responses:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://orderhub.example.com/problems/resource-not-found"));
        problem.setTitle("Resource Not Found");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(/*...*/) {
        // Returns 409 Conflict with retry guidance
    }
}
```

### HTTP Status Code Usage

| Status | Usage                                |
| ------ | ------------------------------------ |
| 200    | Successful GET, PUT                  |
| 201    | Successful POST (resource created)   |
| 204    | Successful DELETE                    |
| 400    | Validation errors                    |
| 401    | Missing/invalid authentication       |
| 403    | Insufficient permissions             |
| 404    | Resource not found                   |
| 409    | Conflict (duplicate, locking, state) |
| 422    | Business logic failure               |
| 500    | Unexpected server errors             |

---

## Configuration Management

### Configuration Hierarchy

```
application.yml          # Base configuration
├── application-dev.yml  # Development overrides
└── application-test.yml # Test overrides
```

### Environment-Specific Settings

```yaml
# application.yml (base)
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-validity-ms: 900000    # 15 minutes
    refresh-token-validity-hours: 168    # 7 days

# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderhub
    username: orderhub
    password: orderhub
  jpa:
    show-sql: true

# application-test.yml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///orderhub
  flyway:
    enabled: true
```

### Environment Variables

| Variable                 | Description                  | Default          |
| ------------------------ | ---------------------------- | ---------------- |
| `JWT_SECRET`             | HMAC signing key (256+ bits) | Dev-only default |
| `DB_URL`                 | PostgreSQL connection URL    | localhost:5432   |
| `DB_USERNAME`            | Database user                | orderhub         |
| `DB_PASSWORD`            | Database password            | orderhub         |
| `SPRING_PROFILES_ACTIVE` | Active profile               | dev              |

---

## Testing Strategy

### Test Pyramid

```
          ┌─────────────┐
          │    E2E      │  ← Few integration scenarios
          │   Tests     │
          ├─────────────┤
          │ Integration │  ← API + DB tests (Testcontainers)
          │   Tests     │
          ├─────────────┤
          │    Unit     │  ← Extensive service layer tests
          │   Tests     │
          └─────────────┘
```

### Test Types

**Unit Tests:**

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock private OrderRepository orderRepository;
    @Mock private InventoryService inventoryService;
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void createOrder_shouldDecrementInventory() {
        // Given, When, Then
    }
}
```

**Integration Tests:**

```java
@SpringBootTest
@Testcontainers
class OrderControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired private MockMvc mockMvc;

    @Test
    void createOrder_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
            .contentType(APPLICATION_JSON)
            .content(orderJson))
            .andExpect(status().isCreated());
    }
}
```

### Test Commands

```bash
# Unit tests only
mvn test

# Integration tests (requires Docker)
mvn verify

# Test with coverage
mvn verify -Pcoverage
```

---

## Deployment Architecture

### Container Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Compose                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────┐         ┌─────────────┐                    │
│  │   backend   │◄───────►│  postgres   │                    │
│  │  (port 8080)│         │ (port 5432) │                    │
│  └──────┬──────┘         └─────────────┘                    │
│         │                                                    │
│         │                                                    │
│  ┌──────┴──────┐                                            │
│  │  frontend   │                                            │
│  │  (port 4200)│                                            │
│  └─────────────┘                                            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Docker Compose Services

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: orderhub
      POSTGRES_USER: orderhub
      POSTGRES_PASSWORD: orderhub
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  backend:
    build: ./backend
    depends_on:
      - db
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/orderhub
    ports:
      - "8080:8080"

  frontend:
    build: ./frontend
    depends_on:
      - backend
    ports:
      - "4200:80"
```

### Build & Deploy Commands

```bash
# Development
docker compose up db -d          # Database only
mvn spring-boot:run             # Run backend locally

# Full stack
docker compose up --build        # Build and start all services

# Production build
mvn clean package -DskipTests   # Create JAR
docker build -t orderhub-backend ./backend
```

---

## Appendix

### Quick Reference: Module Imports

```java
// Common module - available to all
import com.orderhub.common.exception.*;
import com.orderhub.common.config.*;

// Auth module
import com.orderhub.auth.entity.User;
import com.orderhub.auth.entity.Role;
import com.orderhub.auth.repository.UserRepository;

// Catalog module
import com.orderhub.catalog.entity.Product;
import com.orderhub.catalog.repository.ProductRepository;

// Inventory module
import com.orderhub.inventory.service.InventoryService;

// Orders module
import com.orderhub.orders.service.OrderService;
```

### Key Design Decisions

| Decision                  | Rationale                                          |
| ------------------------- | -------------------------------------------------- |
| UUID for primary keys     | Avoids ID guessing, enables distributed generation |
| BigDecimal for money      | Prevents floating-point precision errors           |
| @Version for locking      | Optimistic concurrency without database locks      |
| Interface + Impl services | Dependency inversion, testability                  |
| RFC 7807 errors           | Standard machine-readable error format             |
| Client-side cart          | Simplifies backend, scales horizontally            |
| Idempotency keys          | Safe payment retries                               |

### Performance Considerations

1. **Pagination**: All list endpoints use Spring Data pagination
2. **Lazy Loading**: JPA associations are `FetchType.LAZY` by default
3. **Connection Pooling**: HikariCP (Spring Boot default)
4. **Indexing**: Queries optimized with appropriate indexes
5. **Caching**: Can add Spring Cache for read-heavy endpoints

---

_Document Version: 1.0_  
_Last Updated: February 2026_
