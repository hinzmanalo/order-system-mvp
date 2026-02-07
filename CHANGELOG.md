# Changelog

All notable changes to the OrderHub MVP project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial project scaffolding for backend (Java 17, Spring Boot 3)
- Initial project scaffolding for frontend (Angular 17+)
- Docker Compose infrastructure setup
- PostgreSQL 16 database configuration
- Flyway migration support
- Maven build configuration
- Project documentation structure (PRD, implementation plans, MVP spec)

### In Progress

- Backend module development (common, auth, catalog, inventory, orders, payments)
- Frontend feature development (auth, catalog, cart, checkout, orders, admin)
- Database schema migrations
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
