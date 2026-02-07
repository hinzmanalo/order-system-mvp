# Feature 05: Common Module

**Priority**: Foundation
**Dependencies**: 01-backend-scaffolding, 04-database-schema
**Parallel with**: None
**Blocks**: 06-auth-backend, 07-catalog-backend, 08-inventory-backend

---

## Overview

Implement the shared infrastructure layer used by all backend modules: CORS configuration, OpenAPI/Swagger setup, custom exception classes, and the global exception handler with RFC 7807 Problem Detail responses.

## User Stories

- US-026 (partial): API documentation — OpenAPI config enables Swagger UI

## Tasks

### 5.1 CORS configuration

- [x] Create `com.orderhub.common.config.CorsConfig.java`:
  - `@Configuration` class with `WebMvcConfigurer` implementation
  - Allow origin: `http://localhost:4200`
  - Allow methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
  - Allow headers: `*`
  - Allow credentials: true
  - Max age: 3600

### 5.2 OpenAPI configuration

- [x] Create `com.orderhub.common.config.OpenApiConfig.java`:
  - `@Configuration` with `@OpenAPIDefinition`
  - API title: "OrderHub API"
  - Version: "1.0"
  - Description: "OrderHub Monolith Ordering System REST API"
  - Tags: Auth, Products, Inventory, Orders, Payments, Admin
  - Security scheme: Bearer JWT

### 5.3 Custom exception classes

- [x] `com.orderhub.common.exception.ResourceNotFoundException.java`
  - Extends `RuntimeException`
  - Constructor: `ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue)`
  - Message: `"{resourceName} not found with {fieldName}: {fieldValue}"`
- [x] `com.orderhub.common.exception.DuplicateResourceException.java`
  - Constructor: `DuplicateResourceException(String resourceName, String fieldName, Object fieldValue)`
  - Message: `"{resourceName} already exists with {fieldName}: {fieldValue}"`
- [x] `com.orderhub.common.exception.InsufficientStockException.java`
  - Constructor: `InsufficientStockException(String productName, int available, int requested)`
  - Fields: productName, available, requested (for structured error response)
- [x] `com.orderhub.common.exception.InvalidOrderStateException.java`
  - Constructor: `InvalidOrderStateException(String currentState, String attemptedAction)`
- [x] `com.orderhub.common.exception.PaymentAmountMismatchException.java`
  - Constructor: `PaymentAmountMismatchException(BigDecimal expected, BigDecimal provided)`

### 5.4 Global exception handler

- [x] Create `com.orderhub.common.exception.GlobalExceptionHandler.java`:
  - `@RestControllerAdvice`
  - Uses Spring 6 `ProblemDetail` (RFC 7807) for all responses
  - Handlers:
    - `ResourceNotFoundException` → 404
    - `DuplicateResourceException` → 409
    - `InsufficientStockException` → 409
    - `InvalidOrderStateException` → 409
    - `PaymentAmountMismatchException` → 422
    - `MethodArgumentNotValidException` → 400 (with field-level validation errors)
    - `AccessDeniedException` → 403
    - `OptimisticLockingFailureException` → 409
    - `DataIntegrityViolationException` → 409 (unique constraint violations)
    - Generic `Exception` → 500 (catch-all, log the error)
  - Each handler sets:
    - `type`: URI like `https://orderhub.example.com/problems/{error-type}`
    - `title`: Human-readable error title
    - `status`: HTTP status code
    - `detail`: Specific error message
    - `instance`: Request URI

## Verification

- [x] `mvn clean compile` succeeds with all new classes
- [x] Swagger UI is accessible at `/swagger-ui/index.html` (protected by Spring Security, will be public in Phase 06)
- [x] OpenAPI spec at `/v3/api-docs` responds (protected by Spring Security, will be public in Phase 06)
- [x] CORS configuration applied for `http://localhost:4200`

## Files Created

```
backend/src/main/java/com/orderhub/common/
├── config/
│   ├── CorsConfig.java
│   └── OpenApiConfig.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    ├── InsufficientStockException.java
    ├── InvalidOrderStateException.java
    └── PaymentAmountMismatchException.java
```
