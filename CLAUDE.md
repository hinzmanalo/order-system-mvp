# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**OrderHub MVP** is a modular monolith e-commerce platform built with Spring Boot 3.2.2 (Java 17) and Angular 17.3. It features JWT authentication, role-based access control (USER/ADMIN), atomic inventory management with optimistic locking, and payment processing with idempotency guarantees.

## Build & Run Commands

### Database (required first)

```bash
docker compose up db -d                              # Start PostgreSQL (localhost:5432/orderhub, creds: orderhub/orderhub)
docker exec -it orderhub-db psql -U orderhub -d orderhub  # Direct DB access
```

### Backend

```bash
cd backend
mvn clean compile                                     # Compile
mvn spring-boot:run -Dspring-boot.run.profiles=dev    # Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev,nosecurity  # Run without auth (all endpoints open, mock ADMIN user injected)
mvn test                                              # Run all tests (45 unit tests)
mvn test -Dtest=AuthServiceImplTest                   # Run single test class
mvn verify                                            # Integration tests (needs Docker)
mvn clean package -DskipTests                         # Package JAR
```

### Frontend

```bash
cd frontend
npm install                                           # Install dependencies
ng serve                                              # Dev server on port 4200 (proxies /api to localhost:8080)
ng test                                               # Run tests (Karma + Jasmine)
ng build                                              # Production build
```

### Full Stack

```bash
docker compose up --build                             # Runs PostgreSQL + Spring Boot app
```

### API Docs

Swagger UI at `http://localhost:8080/swagger-ui.html` when backend is running.

## Architecture

### Module Structure

```
backend/src/main/java/com/orderhub/
├── common/          # Shared: CorsConfig, OpenApiConfig, GlobalExceptionHandler, custom exceptions
├── auth/            # JWT auth, Spring Security, user management
│   └── security/    # SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter, DevSecurityConfig
├── catalog/         # Product CRUD, pagination, name/price filtering
├── inventory/       # Stock management with optimistic locking (@Version)
├── orders/          # Atomic order creation (inventory decrement in same transaction)
└── payments/        # Payment processing with Idempotency-Key header

frontend/src/app/
├── core/            # Services, guards (authGuard, adminGuard), interceptors (authInterceptor), models
├── shared/          # Reusable UI components
└── features/        # auth, catalog, cart, checkout, orders, admin
```

### Module Dependency Chain

```
common → auth → catalog → inventory → orders → payments
```

Inter-module calls use direct service injection (same JVM), not REST.

### Key Patterns

- **Entities**: UUID primary keys (`gen_random_uuid()`), `@CreationTimestamp`/`@UpdateTimestamp`, `@Version` on Product and Inventory for optimistic locking
- **DTOs**: Separate Request/Response classes per endpoint; never expose entities via API
- **Services**: Interface + `*Impl` pattern (e.g., `ProductService` interface, `ProductServiceImpl` class)
- **Transactions**: `@Transactional` on write operations in service implementations
- **Exception handling**: Custom exceptions in `common/exception/` → RFC 7807 ProblemDetail responses via `GlobalExceptionHandler`
- **API paths**: All endpoints under `/api/v1/`, admin endpoints under `/api/v1/admin/` with `@PreAuthorize("hasRole('ADMIN')")`
- **Frontend**: Standalone components only (no NgModules), Angular Signals for state, Observables for HTTP, functional guards/interceptors, lazy-loaded routes

### Security Profiles

- **`dev` profile**: Normal JWT auth enabled, connects to Docker PostgreSQL
- **`nosecurity` profile**: Disables JWT auth entirely, injects mock ADMIN user into SecurityContext. Uses `DevSecurityConfig` and `DevAuthenticationFilter`. Activate with `-Dspring-boot.run.profiles=dev,nosecurity`
- **Frontend `authBypass`**: Set in `environment.development.ts` (`authBypass: true`) — skips auth guards and token attachment in dev mode

### Database

- **PostgreSQL 16** via Docker, Flyway migrations in `backend/src/main/resources/db/migration/` (V1-V7)
- **Seed data** (V7): admin@orderhub.com / admin123 (ADMIN), user@orderhub.com / user123 (USER), 5 products with inventory
- Migration naming: `V{N}__{description}.sql`

## Business Rules

1. **Atomic order creation**: Inventory decrements in the same transaction as order creation
2. **Optimistic locking**: Product and Inventory use `@Version` — concurrent updates return 409 Conflict
3. **Payment amount matching**: Must exactly equal order total (`BigDecimal.compareTo() == 0`)
4. **Idempotency**: Payments require `Idempotency-Key` header to prevent duplicates
5. **Order lifecycle**: `CONFIRMED → PAID` or `CONFIRMED → CANCELLED` only (no modification after payment/cancellation)
6. **Order cancellation restores inventory** atomically
7. Only ACTIVE products can be ordered; stock must be available

## Testing

### Backend (45 unit tests, all passing)

- JUnit 5 + Mockito, service-layer focused, >80% coverage
- Test naming: `methodName_scenario_expectedBehavior()`
- Test modules: AuthService (10), ProductService (10), InventoryService (8), OrderService (11), PaymentService (6)

### Frontend (120 specs, 76 passing)

- Jasmine + Karma, component and service specs
- Known issues: JWT token mocking errors in some test specs (runtime works correctly)

### Add

- Add random emoticon when you reply
