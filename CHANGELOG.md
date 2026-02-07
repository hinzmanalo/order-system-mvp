# Changelog

All notable changes to the OrderHub MVP project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Authentication Backend (Feature 06)**:
  - JWT-based authentication with Spring Security
  - User registration and login endpoints with BCrypt password hashing
  - Token refresh mechanism with rotation (old tokens invalidated)
  - Role-based access control (USER/ADMIN roles)
  - JwtTokenProvider for JWT generation and validation using JJWT 0.12.5
  - CustomUserDetailsService for Spring Security integration
  - JwtAuthenticationFilter for Bearer token validation
  - JwtAuthEntryPoint for RFC 7807 authentication error responses
  - SecurityConfig with stateless session management and CSRF disabled
  - AuthController: POST /api/v1/auth/register, /login, /refresh, and GET /me
  - AdminUserController: GET /api/v1/admin/users (list), GET /{id}, PUT /{id}/role
  - User, RefreshToken, and Role entities with JPA mappings
  - UserRepository and RefreshTokenRepository with custom query methods
  - AuthService with complete user management and token operations
  - DTO classes: RegisterRequest, LoginRequest, RefreshRequest, TokenResponse, UserResponse, UpdateRoleRequest
  - ConflictException and UnauthorizedException custom exceptions
  - Global exception handlers for 401 and 409 status codes
  - JWT configuration in application.yml (secret, expiry settings)
  - Public endpoints: /api/v1/auth/**, /api/v1/products/**, /swagger-ui/\*\*, /actuator/health
  - Protected endpoints: /api/v1/** requires authentication, /api/v1/admin/** requires ADMIN role

- Common module infrastructure:
  - CORS configuration for Angular frontend (localhost:4200)
  - OpenAPI/Swagger configuration with API documentation structure
  - Custom exception classes (ResourceNotFound, DuplicateResource, InsufficientStock, InvalidOrderState, PaymentAmountMismatch)
  - Global exception handler with RFC 7807 Problem Detail responses
  - Standardized error handling for all backend modules

### Changed

- Auth module code quality improvements:
  - Added comprehensive SLF4J logging to all auth module classes
  - Implemented parameterized logging for better performance
  - Added appropriate log levels (INFO for business events, WARN for failures, DEBUG for read operations)
  - Enhanced JavaDoc documentation for all public classes and methods
  - Added @param, @return, @throws, @author, @version, and @since tags
  - Documented thread-safety considerations in service classes
  - Added business logic explanations in implementation comments

## [1.0.0] - 2026-02-08

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

- Backend module development (catalog, inventory, orders, payments)
- Frontend feature development (auth, catalog, cart, checkout, orders, admin)
- API documentation with OpenAPI/Swagger
- Unit and integration test suites

---

## Technical Infrastructure

- **Backend**: Java 17, Spring Boot 3, Spring Data JPA, Spring Security, PostgreSQL 16
- **Frontend**: Angular 17+, TypeScript 5.x, Angular Signals, SCSS
- **Database**: PostgreSQL 16 with Flyway migrations
- **Authentication**: JWT (JJWT 0.12.5), BCrypt password hashing, token refresh rotation
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
