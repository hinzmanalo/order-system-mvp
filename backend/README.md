# OrderHub Backend API

A production-ready Spring Boot application providing REST APIs for order management, product catalog, and user authentication.

> **📚 Documentation Hub**
>
> - [Architecture & System Design](docs/ARCHITECTURE.md) - Complete technical architecture
> - [API Quick Reference](docs/API_QUICK_REFERENCE.md) - Endpoint cheat sheet
> - [Developer Guide](docs/DEVELOPER_GUIDE.md) - Development workflow
> - [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) - Production deployment
> - [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues

## Features

- **JWT-based Authentication** - Secure login with access and refresh tokens
- **Product Catalog** - Browse and manage products with filtering and pagination
- **Order Management** - Create and track customer orders with atomic inventory updates
- **Inventory Control** - Real-time stock management with optimistic locking
- **API Documentation** - Interactive Swagger UI for testing endpoints
- **Database Migrations** - Version-controlled schema with Flyway
- **Health Monitoring** - Spring Actuator endpoints for system health

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 16
- Docker and Docker Compose (recommended)

### Run with Docker

```bash
# Start database and application
docker compose up --build

# Application available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

### Run Locally

```bash
# Start PostgreSQL (via Docker)
docker compose up db -d

# Run application
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Application starts on port 8080
```

## Architecture

> **📖 For comprehensive architecture documentation, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**
>
> Covers: Module structure, domain model, data flow diagrams, API design, security architecture,
> database design, concurrency control, error handling, and deployment architecture.

### Module Structure

```
com.orderhub/
├── common/           # Shared utilities, exceptions, config
├── auth/             # User authentication and authorization
├── catalog/          # Product catalog management
├── inventory/        # Stock level management
├── orders/           # Order processing and tracking
└── OrderHubApplication.java
```

### Design Patterns

- **Modular Monolith** - Single deployable unit with clean domain boundaries
- **Interface + Impl Pattern** - Service layer uses interface/implementation
- **DTO Pattern** - Separate request/response objects, entities never exposed
- **Repository Pattern** - Spring Data JPA repositories for data access
- **Optimistic Locking** - `@Version` on Inventory and Product entities
- **Atomic Transactions** - Order creation with inventory decrements in single transaction

### Database Schema

Key entities and relationships:

- **users** - Customer accounts (UUID primary key)
- **products** - Product catalog with pricing
- **inventory** - Stock levels per product (optimistic locking)
- **orders** - Customer orders with lifecycle status
- **order_items** - Line items in each order
- **payments** - Payment transactions (idempotent)
- **refresh_tokens** - JWT refresh token storage

## API Reference

### Authentication

#### Register New User

```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response (201 Created):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "ROLE_USER",
  "createdAt": "2026-02-13T10:30:00"
}
```

#### Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### Refresh Access Token

```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

#### Get Current User

```bash
GET /api/v1/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response:**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "ROLE_USER",
  "createdAt": "2026-02-13T10:30:00"
}
```

### Product Catalog

#### Browse Products

```bash
GET /api/v1/products?page=0&size=20&sort=name,asc
```

**Query Parameters:**

| Parameter  | Type    | Description                                   |
| ---------- | ------- | --------------------------------------------- |
| `name`     | string  | Filter by product name (case-insensitive)     |
| `minPrice` | decimal | Minimum price filter (inclusive)              |
| `maxPrice` | decimal | Maximum price filter (inclusive)              |
| `page`     | integer | Page number (default: 0)                      |
| `size`     | integer | Page size (default: 20)                       |
| `sort`     | string  | Sort field and direction (e.g., `price,desc`) |

**Response:**

```json
{
  "content": [
    {
      "id": "prod-123",
      "name": "Wireless Headphones",
      "description": "Premium noise-cancelling headphones",
      "price": 199.99,
      "sku": "WH-1000XM5",
      "active": true,
      "availableStock": 50,
      "createdAt": "2026-01-15T09:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

#### Get Product by ID

```bash
GET /api/v1/products/{productId}
```

**Response:**

```json
{
  "id": "prod-123",
  "name": "Wireless Headphones",
  "description": "Premium noise-cancelling headphones",
  "price": 199.99,
  "sku": "WH-1000XM5",
  "active": true,
  "availableStock": 50,
  "createdAt": "2026-01-15T09:00:00"
}
```

### Orders

#### Create Order

```bash
POST /api/v1/orders
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "items": [
    {
      "productId": "prod-123",
      "quantity": 2
    },
    {
      "productId": "prod-456",
      "quantity": 1
    }
  ]
}
```

**Response (201 Created):**

```json
{
  "id": "order-789",
  "userId": "user-123",
  "status": "CONFIRMED",
  "totalAmount": 599.97,
  "items": [
    {
      "productId": "prod-123",
      "productName": "Wireless Headphones",
      "quantity": 2,
      "priceAtOrder": 199.99
    }
  ],
  "createdAt": "2026-02-13T11:00:00"
}
```

#### Get My Orders

```bash
GET /api/v1/orders?page=0&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Query Parameters:**

| Parameter   | Type      | Description                                   |
| ----------- | --------- | --------------------------------------------- |
| `status`    | string    | Filter by status (CONFIRMED, PAID, CANCELLED) |
| `startDate` | date-time | Filter orders from this date                  |
| `endDate`   | date-time | Filter orders until this date                 |

#### Get Order by ID

```bash
GET /api/v1/orders/{orderId}
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

#### Cancel Order

```bash
POST /api/v1/orders/{orderId}/cancel
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response:**

```json
{
  "id": "order-789",
  "status": "CANCELLED",
  "message": "Order cancelled successfully. Inventory has been restored."
}
```

### Admin Endpoints

#### Create Product (Admin Only)

```bash
POST /api/v1/admin/products
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "name": "New Product",
  "description": "Product description",
  "price": 99.99,
  "sku": "SKU-001",
  "initialStock": 100
}
```

#### Update Inventory (Admin Only)

```bash
PUT /api/v1/admin/inventory/{productId}
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "quantity": 150,
  "version": 5
}
```

## Error Handling

The API returns RFC 7807 Problem Detail responses for errors.

### Error Response Format

```json
{
  "type": "https://orderhub.com/errors/product-not-found",
  "title": "Product Not Found",
  "status": 404,
  "detail": "Product with ID prod-999 does not exist",
  "instance": "/api/v1/products/prod-999",
  "timestamp": "2026-02-13T11:30:00Z"
}
```

### Common Error Codes

| Status | Error Type       | Description                                           |
| ------ | ---------------- | ----------------------------------------------------- |
| 400    | Validation Error | Invalid request data                                  |
| 401    | Unauthorized     | Missing or invalid authentication token               |
| 403    | Forbidden        | Insufficient permissions                              |
| 404    | Not Found        | Resource does not exist                               |
| 409    | Conflict         | Optimistic locking failure or business rule violation |
| 500    | Internal Error   | Unexpected server error                               |

### Example Error Scenarios

**Insufficient Stock:**

```json
{
  "type": "https://orderhub.com/errors/insufficient-stock",
  "title": "Insufficient Stock",
  "status": 409,
  "detail": "Product 'Wireless Headphones' has only 5 units available, but 10 were requested"
}
```

**Optimistic Locking Conflict:**

```json
{
  "type": "https://orderhub.com/errors/optimistic-lock",
  "title": "Resource Modified",
  "status": 409,
  "detail": "Inventory was modified by another transaction. Please retry."
}
```

## Configuration

### Environment Variables

| Variable                     | Required | Default                 | Description                               |
| ---------------------------- | -------- | ----------------------- | ----------------------------------------- |
| `JWT_SECRET`                 | No       | dev-secret              | Secret key for JWT signing (min 256 bits) |
| `SPRING_DATASOURCE_URL`      | No       | localhost:5432/orderhub | PostgreSQL connection URL                 |
| `SPRING_DATASOURCE_USERNAME` | No       | orderhub                | Database username                         |
| `SPRING_DATASOURCE_PASSWORD` | No       | orderhub                | Database password                         |

### Application Profiles

**Development (`dev`):**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- SQL logging enabled
- Database: localhost:5432/orderhub
- Sample data seeded via V7\_\_seed_dev_data.sql

**Test (`test`):**

```bash
mvn test
```

- Uses H2 in-memory database for unit tests
- Testcontainers PostgreSQL for integration tests

**Production:**

```bash
java -jar orderhub.jar --spring.profiles.active=prod
```

- SQL logging disabled
- Requires production database configuration

### JWT Configuration

Configure in `application.yml`:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-validity-ms: 900000 # 15 minutes
    refresh-token-validity-hours: 168 # 7 days
```

## Development

### Build

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Integration tests (requires Docker)
mvn verify

# Package JAR
mvn clean package
```

### Database Migrations

Migrations are in `src/main/resources/db/migration/`:

```
V1__create_users_table.sql
V2__create_products_table.sql
V3__create_inventory_table.sql
V4__create_orders_tables.sql
V5__create_payments_table.sql
V6__create_refresh_tokens_table.sql
V7__seed_dev_data.sql
```

**Create a new migration:**

1. Create file: `V{N}__description.sql`
2. Write SQL DDL statements
3. Restart application (Flyway auto-applies)

**Check migration status:**

```bash
mvn flyway:info
```

### Code Structure Guidelines

**Controllers:**

- Base path: `/api/v1/`
- Use `@Valid` for request validation
- Return `ResponseEntity` with appropriate status codes
- Document with `@Operation` and `@ApiResponse`

**Services:**

- Interface + `*Impl` pattern
- `@Transactional` on write operations
- Never expose entities, use DTOs

**Entities:**

- UUID primary keys (`@Id @GeneratedValue`)
- `@CreationTimestamp` and `@UpdateTimestamp`
- `@Version` for optimistic locking on Inventory/Product

**DTOs:**

- Separate Request/Response classes
- Jakarta validation annotations (`@NotNull`, `@Email`, etc.)
- Immutable where possible (use records for Java 17+)

### Testing

**Unit Tests:**

```bash
mvn test
```

**Integration Tests:**

```bash
# Starts Testcontainers PostgreSQL
mvn verify
```

**Test Coverage:**

```bash
mvn clean verify jacoco:report
# Report: target/site/jacoco/index.html
```

## API Documentation

### Swagger UI

Access interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

Features:

- Browse all endpoints
- Test requests with sample data
- View request/response schemas
- Authentication flow examples

### OpenAPI Spec

Download OpenAPI 3.0 specification:

```
http://localhost:8080/v3/api-docs
```

## Health & Monitoring

### Health Endpoint

```bash
GET http://localhost:8080/actuator/health
```

**Response:**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### Application Info

```bash
GET http://localhost:8080/actuator/info
```

## Business Rules

### Critical Constraints

1. **Atomic Order Creation** - Inventory decrements happen in the same transaction as order creation. If order fails, inventory rollback is automatic.

2. **Optimistic Locking** - Inventory and Product entities use `@Version`. Concurrent updates return 409 Conflict. Client must retry with fresh data.

3. **Payment Validation** - Payment amount must exactly match order total (`BigDecimal.compareTo() == 0`).

4. **Idempotency** - Payment endpoints require `Idempotency-Key` header to prevent duplicate processing.

5. **Order Lifecycle** - Valid transitions:
   - `CONFIRMED` → `PAID` (on successful payment)
   - `CONFIRMED` → `CANCELLED` (inventory restored)
   - `PAID` orders cannot be cancelled

### Validation Rules

**User Registration:**

- Email must be unique
- Password minimum 8 characters
- Name required

**Product Creation:**

- SKU must be unique
- Price must be positive
- Initial stock ≥ 0

**Order Creation:**

- All products must exist and be active
- Sufficient inventory required
- Minimum 1 item per order

## Troubleshooting

### Application Won't Start

**Error:** `Failed to configure a DataSource`

**Cause:** PostgreSQL not running or connection details incorrect

**Solution:**

```bash
# Start PostgreSQL via Docker
docker compose up db -d

# Verify connection
psql -h localhost -U orderhub -d orderhub
# Password: orderhub
```

### JWT Token Expired

**Error:** `401 Unauthorized - Token expired`

**Cause:** Access token validity is 15 minutes

**Solution:**

```bash
# Use refresh token to get new access token
POST /api/v1/auth/refresh
{
  "refreshToken": "your-refresh-token"
}
```

### Optimistic Locking Failure

**Error:** `409 Conflict - Optimistic lock exception`

**Cause:** Entity was modified by another transaction

**Solution:**

```bash
# Fetch fresh data
GET /api/v1/admin/inventory/{productId}

# Retry update with new version
PUT /api/v1/admin/inventory/{productId}
{
  "quantity": 100,
  "version": 6  # Use version from GET response
}
```

### Migration Checksum Mismatch

**Error:** `FlywayException: Migration checksum mismatch`

**Cause:** Existing migration file was modified

**Solution:**

```bash
# Development only - reset database
docker compose down -v
docker compose up db -d

# Production - create repair migration
mvn flyway:repair
```

### Port Already in Use

**Error:** `Port 8080 is already in use`

**Solution:**

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 {PID}

# Or change port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## Security

### Authentication Flow

1. User registers or logs in via `/api/v1/auth/register` or `/api/v1/auth/login`
2. Server returns JWT access token (15 min) and refresh token (7 days)
3. Client includes access token in `Authorization: Bearer {token}` header
4. When access token expires, use `/api/v1/auth/refresh` with refresh token
5. Refresh tokens are stored in database and can be revoked

### Password Requirements

- Minimum 8 characters
- BCrypt hashing with strength 10
- Passwords never logged or exposed in responses

### Role-Based Access

- `ROLE_USER` - Create orders, view own data
- `ROLE_ADMIN` - Manage products, inventory, view all orders

### Security Headers

Configured via Spring Security:

- CSRF protection disabled (stateless JWT)
- XSS protection enabled
- CORS configured for frontend origin

## Performance

### Database Indexing

Key indexes for query performance:

- `users(email)` - Unique index for login
- `products(sku)` - Unique index for SKU lookup
- `orders(user_id, created_at)` - Composite index for user order history
- `order_items(order_id)` - Foreign key index

### Pagination

All list endpoints support pagination:

```bash
# Default page size: 20
GET /api/v1/products?page=0&size=50

# Maximum page size: 100
```

### Optimistic Locking

Prevents lost updates on concurrent modifications:

- Inventory quantity changes
- Product updates
- No database locks required

## License

Copyright © 2026 OrderHub. All rights reserved.

---

**Need help?** Check [Swagger UI](http://localhost:8080/swagger-ui.html) for interactive API testing 😊
