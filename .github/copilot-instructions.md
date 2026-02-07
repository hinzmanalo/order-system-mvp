# OrderHub MVP - AI Agent Instructions

## Tech Stack

- **Backend**: Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL 16, Flyway, Maven
- **Frontend**: Angular 17+ (standalone components), TypeScript 5.x, Angular Signals, SCSS
- **Infrastructure**: Docker Compose (`docker compose up --build`)

## Project Structure

```
backend/src/main/java/com/orderhub/{module}/
├── controller/    # REST endpoints (@RestController, /api/v1/*)
├── dto/           # Request/Response DTOs with Jakarta validation
├── entity/        # JPA entities (UUID PKs, @Version for optimistic locking)
├── repository/    # Spring Data JPA interfaces
└── service/       # Interface + Impl pattern (@Transactional on writes)

frontend/src/app/
├── core/          # Services, guards, interceptors, models
├── shared/        # Reusable UI components
└── features/      # auth, catalog, cart, checkout, orders, admin
```

## Build & Test Commands

```bash
# Backend
cd backend && mvn clean compile                    # Compile
mvn spring-boot:run -Dspring-boot.run.profiles=dev # Run dev
mvn test                                           # Unit tests
mvn verify                                         # Integration tests (needs Docker)

# Frontend
cd frontend && npm install && ng serve             # Dev server (port 4200)
ng test                                            # Run tests

# Docker
docker compose up db -d                            # Database only
docker compose up --build                          # Full stack
```

## Key Conventions

### Backend (Java)

- **Entities**: UUID primary keys, `@CreationTimestamp`/`@UpdateTimestamp`, `@Version` on Inventory/Product
- **DTOs**: Separate Request/Response classes, never expose entities directly
- **Services**: Interface + `*Impl` pattern, `@Transactional` on write operations
- **Exceptions**: Throw from `common.exception` package → RFC 7807 ProblemDetail responses
- **Controllers**: Base path `/api/v1/`, use `@Valid`, document with `@Operation`/`@ApiResponse`

### Frontend (Angular)

- **Standalone components** only (no NgModules)
- **Signals** for state management, **Observables** for HTTP
- **Functional guards** (`CanActivateFn`) and **interceptors** (`HttpInterceptorFn`)
- Lazy-loaded routes, reactive forms

### Database

- Migrations: `backend/src/main/resources/db/migration/V{N}__description.sql`
- Dev DB: `localhost:5432/orderhub` (user: orderhub, pass: orderhub)

## Business Rules (Critical)

1. Order creation is **atomic** – inventory decrements in same transaction
2. **Optimistic locking** on Inventory/Product (409 on conflict)
3. Payment amount must **exactly match** order total (`BigDecimal.compareTo() == 0`)
4. **Idempotency-Key header** required for payments (prevents duplicate processing)
5. Order lifecycle: `CONFIRMED → PAID` or `CONFIRMED → CANCELLED`

## Module Dependencies

```
common → auth → catalog → inventory → orders → payments
```

Inter-module calls use direct service injection (same JVM), not REST.

## Documentation

- Implementation plans: [docs/plans/](../docs/plans/) (phases 01-16)
- PRD: [docs/prd.md](../docs/prd.md)
- MVP spec: [docs/OrderHub_MVP.md](../docs/OrderHub_MVP.md)

## others

Always put smile emkoji at the end of the message.
