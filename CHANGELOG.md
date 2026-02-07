# Changelog

All notable changes to the OrderHub MVP project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0]

### Added

- Initial project scaffolding for backend (Java 17, Spring Boot 3)
- Docker Compose infrastructure with PostgreSQL 16 database
- Multi-stage Dockerfile for Spring Boot application
- Docker health checks for database readiness
- Persistent volume configuration for PostgreSQL data
- .dockerignore file for optimized builds
- Maven build configuration
- Flyway migration support
- Angular 17.3 frontend project with standalone components
- Frontend proxy configuration for backend API (proxy.conf.json)
- Environment files for development and production configurations
- SCSS styling support and routing setup
- Frontend .gitignore entries (node_modules, dist, .angular)
- Cleaned up default Angular template with router-outlet
- Project documentation structure (PRD, implementation plans, MVP spec)
- Complete database schema with 7 Flyway migrations:
  - V1: Users table with UUID PKs, email uniqueness, role-based access
  - V2: Products table with SKU uniqueness, price validation, optimistic locking
  - V3: Inventory table with FK to products, quantity constraints
  - V4: Orders and order_items tables with proper indexes and cascade deletes
  - V5: Payments table with idempotency_key for duplicate prevention
  - V6: Refresh tokens table for JWT authentication
  - V7: Development seed data (2 users, 5 products with inventory)

### In Progress

- Backend module development (common, auth, catalog, inventory, orders, payments)
- Frontend feature development (auth, catalog, cart, checkout, orders, admin)
- API documentation with OpenAPI/Swagger
- Unit and integration test suites

## [0.1.0] - TBD

### Planned Features

- User authentication and authorization (JWT-based)
- Product catalog management
- Shopping cart functionality
- Order processing with atomic inventory updates
- Payment processing with idempotency support
- Admin dashboard for inventory and order management
- Optimistic locking for inventory and products
- RESTful API endpoints (base path: `/api/v1/`)
- Responsive Angular UI with standalone components
- Signal-based state management

### Technical Infrastructure

- **Backend**: Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL 16
- **Frontend**: Angular 17+, TypeScript 5.x, Angular Signals, SCSS
- **Database**: PostgreSQL 16 with Flyway migrations
- **Containerization**: Docker Compose
- **Build Tools**: Maven (backend), npm/Angular CLI (frontend)

---

## Release Notes Template

For future releases, use the following categories:

### Added

- New features

### Changed

- Changes in existing functionality

### Deprecated

- Soon-to-be removed features

### Removed

- Removed features

### Fixed

- Bug fixes

### Security

- Security improvements and vulnerability fixes
