# OrderHub MVP

A portfolio-grade monolithic ordering system demonstrating enterprise-level full-stack development with Spring Boot 3 and Angular 17+.

## Overview

OrderHub is a modular monolith e-commerce platform showcasing real-world backend engineering skills including:

- **JWT-based authentication** with Spring Security and token refresh rotation
- **Role-based access control** (USER/ADMIN) with method-level security
- Transactional integrity with atomic inventory management
- Optimistic locking for concurrent updates
- Payment processing with idempotency guarantees
- Clean domain separation with modular architecture
- RESTful API design with OpenAPI documentation

## Tech Stack

### Backend

- **Runtime**: Java 17
- **Framework**: Spring Boot 3.2.2
- **Database**: PostgreSQL 16
- **ORM**: Spring Data JPA / Hibernate
- **Migrations**: Flyway
- **Security**: Spring Security, JWT (jjwt 0.12.5)
- **Documentation**: SpringDoc OpenAPI 3
- **Build Tool**: Maven

### Frontend

- **Framework**: Angular 17.3 (standalone components)
- **Language**: TypeScript 5.x
- **State Management**: Angular Signals
- **HTTP**: Observables
- **Styling**: SCSS
- **Build Tool**: Angular CLI

### Infrastructure

- **Containerization**: Docker & Docker Compose
- **Database**: PostgreSQL 16 (containerized)
- **Local Development**: Docker Compose with health checks

## Quick Start

### Prerequisites

- **Java 17+** (for backend development)
- **Node.js 18+** and npm (for frontend development)
- **Docker** and Docker Compose (for database)
- **Maven** (for backend builds)

### Setup & Run

#### 1. Start PostgreSQL Database

```bash
docker compose up db -d
```

The database will be available at `localhost:5432/orderhub` with credentials `orderhub/orderhub`.

#### 2. Run Backend (Spring Boot)

```bash
cd backend
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend API will be available at `http://localhost:8080`.

**API Documentation**: `http://localhost:8080/swagger-ui.html`

**Authentication Endpoints**:

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login (returns JWT tokens)
- `POST /api/v1/auth/refresh` - Refresh access token
- `GET /api/v1/auth/me` - Get current user profile

**Admin Endpoints** (requires ADMIN role):

- `GET /api/v1/admin/users` - List all users
- `GET /api/v1/admin/users/{id}` - Get user by ID
- `PUT /api/v1/admin/users/{id}/role` - Update user role

#### 3. Run Frontend (Angular)

```bash
cd frontend
npm install
npm run start
# or: ng serve
```

Frontend will be available at `http://localhost:4200/`. The application will automatically reload if you change any source files.

**Generated with**: [Angular CLI](https://github.com/angular/angular-cli) version 17.3.17

#### 4. Run Full Stack with Docker

```bash
docker compose up --build
```

This starts both PostgreSQL and the Spring Boot application in containers.

## Project Structure

```
order-system-mvp/
├── backend/                           # Spring Boot backend
│   ├── src/main/java/com/orderhub/
│   │   ├── OrderHubApplication.java  # Main entry point
│   │   ├── common/                   # Shared infrastructure
│   │   │   ├── config/              # CORS, OpenAPI configuration
│   │   │   └── exception/           # Custom exceptions, global handler
│   │   ├── auth/                    # Authentication module
│   │   │   ├── controller/          # AuthController, AdminUserController
│   │   │   ├── dto/                 # Request/Response DTOs
│   │   │   ├── entity/              # User, RefreshToken, Role enum
│   │   │   ├── repository/          # UserRepository, RefreshTokenRepository
│   │   │   ├── service/             # AuthService interface + implementation
│   │   │   └── security/            # Spring Security config, JWT provider
│   │   ├── catalog/                 # Product catalog module
│   │   ├── inventory/               # Inventory management module
│   │   ├── orders/                  # Order processing module
│   │   └── payments/                # Payment processing module
│   ├── src/main/resources/
│   │   ├── db/migration/            # Flyway migrations (V1-V7)
│   │   ├── application.yml          # Base configuration
│   │   ├── application-dev.yml      # Development profile
│   │   └── application-test.yml     # Test profile
│   ├── pom.xml                      # Maven dependencies
│   └── Dockerfile                   # Multi-stage Docker build
├── frontend/                         # Angular frontend
│   ├── src/app/
│   │   ├── core/                    # Services, guards, interceptors
│   │   ├── shared/                  # Reusable UI components
│   │   └── features/                # Feature modules
│   │       ├── auth/               # Login, registration
│   │       ├── catalog/            # Product browsing
│   │       ├── cart/               # Shopping cart
│   │       ├── checkout/           # Order placement
│   │       ├── orders/             # Order history
│   │       └── admin/              # Admin dashboard
│   ├── proxy.conf.json             # Dev proxy to backend API
│   └── angular.json                # Angular configuration
├── docs/                            # Project documentation
│   ├── prd.md                      # Product Requirements Document
│   ├── OrderHub_MVP.md             # MVP specification
│   ├── implementation.md           # Implementation guide
│   ├── project_status.md           # Current progress tracking
│   └── plans/                      # Feature implementation plans
│       ├── 00-overview.md
│       ├── 01-backend-scaffolding.md
│       ├── 04-database-schema.md
│       └── ... (16 total features)
├── docker-compose.yml              # Multi-service orchestration
└── CHANGELOG.md                    # Version history
```

## Database Schema

The database includes 7 Flyway migrations:

- **V1**: Users table (email, password_hash, role, timestamps)
- **V2**: Products table (UUID PK, SKU, price, optimistic locking)
- **V3**: Inventory table (FK to products, quantity, version)
- **V4**: Orders & order_items tables (user orders with line items)
- **V5**: Payments table (with idempotency_key)
- **V6**: Refresh tokens table (for JWT refresh flow)
- **V7**: Dev seed data (2 users, 5 products with inventory)

### Seed Users

- **Admin**: `admin@orderhub.com` / `admin123` (role: ADMIN)
- **User**: `user@orderhub.com` / `user123` (role: USER)

### Direct Database Access

```bash
docker exec -it orderhub-db psql -U orderhub -d orderhub
```

## Backend Commands

```bash
# Navigate to backend
cd backend

# Compile
mvn clean compile

# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run unit tests
mvn test

# Run integration tests (requires Docker)
mvn verify

# Package as JAR
mvn clean package -DskipTests
```

## Frontend Commands

```bash
# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Development server (port 4200)
ng serve
# or: npm run start
# Navigate to http://localhost:4200/ - auto-reloads on file changes

# Build for production
ng build
# Build artifacts stored in dist/ directory

# Run unit tests (Karma + Jasmine)
ng test

# Run end-to-end tests
ng e2e
# Note: Requires e2e testing package (Playwright, Cypress, etc.)

# Code scaffolding
ng generate component component-name
ng generate directive|pipe|service|class|guard|interface|enum

# Generate standalone component (recommended)
ng generate component features/my-component --standalone

# Get help
ng help
# Or visit: https://angular.io/cli
```

**Note**: The backend API must be running at `http://localhost:8080` for API calls to work (configured in `proxy.conf.json`).

## API Endpoints

### Authentication (`/api/v1/auth`) ✅ Implemented

- `POST /register` - User registration
- `POST /login` - Login (returns access + refresh tokens)
- `POST /refresh` - Refresh access token
- `GET /me` - Get current user profile

### Admin - Users (`/api/v1/admin/users`) ✅ Implemented

- `GET /` - List all users (paginated, ADMIN only)
- `GET /{id}` - Get user by ID (ADMIN only)
- `PUT /{id}/role` - Update user role (ADMIN only)

### Products (`/api/v1/products`) - Planned

- `GET /` - List products (pagination, filtering)
- `GET /{id}` - Get product details
- `POST /` - Create product (ADMIN only)
- `PUT /{id}` - Update product (ADMIN only)
- `DELETE /{id}` - Deactivate product (ADMIN only)

### Inventory (`/api/v1/inventory`) - Planned

- `GET /product/{productId}` - Get stock level
- `PATCH /{id}` - Adjust stock (ADMIN only)

### Orders (`/api/v1/orders`) - Planned

- `POST /` - Create order (atomic with inventory decrement)
- `GET /` - List user's orders
- `GET /{id}` - Get order details
- `PUT /{id}/cancel` - Cancel order (restores inventory)

### Payments (`/api/v1/payments`) - Planned

- `POST /` - Process payment (requires Idempotency-Key header)
- `GET /order/{orderId}` - Get payment status

## Key Conventions

### Backend (Java)

- **UUID Primary Keys**: All entities use `UUID` with `gen_random_uuid()`
- **Timestamps**: `@CreationTimestamp` / `@UpdateTimestamp` on entities
- **Optimistic Locking**: `@Version` on Product and Inventory entities
- **DTOs**: Separate Request/Response classes, never expose entities directly
- **Services**: Interface + `*Impl` pattern (e.g., `ProductService` + `ProductServiceImpl`)
- **Transactions**: `@Transactional` on write operations
- **Exception Handling**: Custom exceptions → RFC 7807 ProblemDetail responses
- **API Paths**: Base path `/api/v1/`, versioned endpoints
- **Validation**: Jakarta Bean Validation on DTOs with `@Valid`

### Frontend (Angular)

- **Standalone Components**: No NgModules, all components are standalone
- **Signals**: Preferred for local state management
- **Observables**: Used for HTTP and asynchronous streams
- **Functional Guards**: `CanActivateFn` instead of class-based guards
- **Functional Interceptors**: `HttpInterceptorFn` pattern
- **Lazy Loading**: Feature routes loaded on demand
- **Reactive Forms**: `FormGroup` and `FormControl` for form handling

### Database

- **Migration Naming**: `V{N}__{description}.sql` (e.g., `V1__create_users_table.sql`)
- **Constraints**: Foreign keys, unique constraints, check constraints in SQL
- **Indexes**: Created for FK columns and frequently queried fields
- **Cascades**: `ON DELETE CASCADE` for dependent data (e.g., order_items)

## Business Rules (Critical)

1. **Atomic Order Creation**: Inventory decrements happen in the same transaction as order creation
2. **Optimistic Locking**: Product and Inventory updates use version-based concurrency control (409 on conflict)
3. **Payment Amount Matching**: Payment amount must exactly match order total (`BigDecimal.compareTo() == 0`)
4. **Idempotency**: Payment requests require `Idempotency-Key` header to prevent duplicate processing
5. **Order Lifecycle**: `CONFIRMED` → `PAID` or `CONFIRMED` → `CANCELLED` (no payment after cancellation)

## Module Dependencies

```
common → auth → catalog → inventory → orders → payments
```

Inter-module communication uses direct service injection (same JVM), not REST calls.

## Development Workflow

### Adding a New Flyway Migration

1. Create `backend/src/main/resources/db/migration/V{next_number}__{description}.sql`
2. Write SQL DDL (CREATE TABLE, ALTER TABLE, etc.)
3. Restart backend - Flyway auto-applies new migrations
4. Verify with: `SELECT * FROM flyway_schema_history;`

### Creating a New Backend Module

1. Create package structure: `com.orderhub.{module}/{controller,dto,entity,repository,service}`
2. Define JPA entity with UUID PK and timestamps
3. Create repository interface extending `JpaRepository`
4. Implement service interface + implementation class
5. Create DTOs for request/response (never expose entities)
6. Implement REST controller with `@RestController` and `/api/v1/{module}` base path
7. Add OpenAPI annotations (`@Operation`, `@ApiResponse`)

### Creating a New Frontend Feature

1. Generate feature component: `ng g c features/{feature-name} --standalone`
2. Add route to `app.routes.ts` (lazy-loaded if possible)
3. Create service: `ng g s features/{feature-name}/services/{feature-name}`
4. Define TypeScript models in `core/models/`
5. Implement component logic with Signals for state
6. Style with SCSS in component file

## Testing

### Backend Tests (Planned)

- **Unit Tests**: Service layer logic with Mockito
- **Integration Tests**: Testcontainers for PostgreSQL, `@SpringBootTest` with test profile
- **Repository Tests**: `@DataJpaTest` for JPA queries

### Frontend Tests (Planned)

- **Unit Tests**: Karma + Jasmine for components and services
- **E2E Tests**: Playwright or Cypress for critical user flows

## Documentation

- **PRD**: [docs/prd.md](docs/prd.md) - Product Requirements Document
- **MVP Spec**: [docs/OrderHub_MVP.md](docs/OrderHub_MVP.md) - MVP feature scope
- **Implementation Plans**: [docs/plans/](docs/plans/) - 16 feature-by-feature plans
- **Project Status**: [docs/project_status.md](docs/project_status.md) - Current progress (6/16 complete)
- **Changelog**: [CHANGELOG.md](CHANGELOG.md) - Version history

## Current Status

**Progress**: 6 / 16 features complete (38%)

### ✅ Completed

- Feature 01: Backend Scaffolding (Spring Boot 3, Maven)
- Feature 02: Docker Infrastructure (PostgreSQL, Docker Compose)
- Feature 03: Angular Scaffolding (Angular 17.3, proxy config)
- Feature 04: Database Schema (7 Flyway migrations + seed data)
- Feature 05: Common Module (CORS, OpenAPI, exception handlers)
- Feature 06: Auth Backend (JWT, Spring Security, user registration/login)

### 🚧 Next Up

- Feature 07: Catalog Backend (Product CRUD, pagination)
- Feature 08: Inventory Backend (Stock management, optimistic locking)

- Feature 05: Common Module (CORS, OpenAPI, exception handling)
- Feature 06: Auth Backend (JWT, Spring Security, registration/login)

## License

This is a portfolio/demonstration project.

## Contact

For questions or feedback about this project, please refer to the documentation in the `docs/` directory.
