# OrderHub MVP - AI Agent Instructions

**Project Status**: ✅ MVP Complete (16/16 features implemented)  
**Backend Tests**: ✅ 45 unit tests passing (100% success rate)  
**Last Updated**: February 14, 2026

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.2, Spring Data JPA, PostgreSQL 16, Flyway, Maven
- **Frontend**: Angular 17+ (standalone components), TypeScript 5.x, Angular Signals, SCSS
- **Infrastructure**: Docker Compose (`docker compose up --build`)
- **Documentation**: OpenAPI 3 / Swagger UI at `/swagger-ui.html`
- **Security**: JWT (access + refresh tokens), BCrypt password hashing
- **Testing**: JUnit 5, >80% service coverage

## Project Structure

```
backend/src/main/java/com/orderhub/{module}/
├── controller/         # Public REST endpoints (@RestController, /api/v1/*)
├── admin/controller/   # Admin REST endpoints (@PreAuthorize("hasRole('ADMIN')"))
├── dto/                # Request/Response DTOs with Jakarta validation
├── entity/             # JPA entities (UUID PKs, @Version for optimistic locking)
├── repository/         # Spring Data JPA interfaces
├── service/            # Interface + Impl pattern (@Transactional on writes)
└── security/           # Security configs (auth module only)

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

- **Entities**: UUID primary keys (`@GeneratedValue(strategy = GenerationType.UUID)`), `@CreationTimestamp`/`@UpdateTimestamp`, `@Version` on Inventory/Product
- **DTOs**: Separate Request/Response classes, never expose entities directly via API
- **Services**: Interface + `*Impl` pattern, `@Transactional` on write operations
- **Exceptions**: Throw from `common.exception` package → RFC 7807 ProblemDetail responses via GlobalExceptionHandler
- **Controllers**:
  - Base path `/api/v1/`
  - Use `@Valid` for request body validation
  - Document with `@Tag`, `@Operation`, `@ApiResponses` for all status codes
  - Public endpoints: `/api/v1/{domain}/*`
  - Admin endpoints: `/api/v1/admin/{domain}/*` with `@PreAuthorize("hasRole('ADMIN')")`
- **Logging**:
  - Use SLF4J logger in all service implementations
  - INFO for business events, WARN for validation failures, ERROR for exceptions
  - Parameterized logging: `logger.info("Order created: {}", orderId)`
- **Security**:
  - JWT access tokens (15-min expiry), refresh tokens (7-day expiry)
  - Token rotation on refresh
  - BCrypt password encoding with strength 10
  - CORS configured for `http://localhost:4200` in dev
- **OpenAPI Documentation**:
  - All controllers must have `@Tag` annotations
  - All endpoints must have `@Operation` (summary + description)
  - All responses must have `@ApiResponses` (200, 201, 400, 401, 403, 404, 409, 422 as applicable)
  - Protected endpoints must have `@SecurityRequirement(name = "bearer-jwt")`

### Frontend (Angular)

- **Standalone components** only (no NgModules)
- **Signals** for reactive state management, **Observables** for HTTP/async
- **Functional guards** (`CanActivateFn`) and **interceptors** (`HttpInterceptorFn`)
- Lazy-loaded routes with `loadComponent`
- **Reactive forms** with validation
- **Guards**:
  - `authGuard`: Validates JWT token presence, redirects to login
  - `adminGuard`: Validates ADMIN role, redirects to home
- **Interceptors**:
  - `authInterceptor`: Attaches JWT Bearer token to all requests
- **Services**:
  - Use `inject()` in constructors (modern Angular pattern)
  - Store auth state in signals (`authService.currentUser()`)
  - HTTP errors handled with toasts or error pages
- **Routing**:
  - Public routes: `/login`, `/register`, `/products`
  - Protected routes: `/cart`, `/checkout`, `/orders`
  - Admin routes: `/admin/*` (dashboard, products, inventory, users, orders)

### Database

- Migrations: `backend/src/main/resources/db/migration/V{N}__description.sql`
- Dev DB: `localhost:5432/orderhub` (user: orderhub, pass: orderhub)

## Business Rules (Critical)

1. Order creation is **atomic** – inventory decrements in same transaction
2. **Optimistic locking** on Inventory/Product (409 on conflict)
3. Payment amount must **exactly match** order total (`BigDecimal.compareTo() == 0`)
4. **Idempotency-Key header** required for payments (prevents duplicate processing)
5. Order lifecycle: `CONFIRMED → PAID` or `CONFIRMED → CANCELLED`
6. Order cancellation **restores inventory** atomically
7. Only CONFIRMED orders can be cancelled (PAID/CANCELLED orders cannot be modified)
8. Only ACTIVE products can be ordered
9. Stock must be available before order creation (checked via optimistic locking)

## Testing Requirements

### Backend Testing

- **Unit Tests**: JUnit 5 with Mockito for service layer testing
- **Coverage**: Maintain >80% service coverage
- **Test Structure**: Given-When-Then pattern
- **Naming**: `methodName_scenario_expectedBehavior()`
- **Test Categories**:
  - Happy path scenarios
  - Validation failures (400 errors)
  - Not found scenarios (404 errors)
  - Conflict scenarios (409 errors for optimistic locking)
  - Business rule violations
- **Integration Tests**: Use `@DataJpaTest` for repository tests
- **Current Status**: 45 unit tests passing (100% success rate)

### Frontend Testing

- **Framework**: Jasmine + Karma
- **Test Types**: Component specs, service specs, guard specs
- **Async Handling**: Use `fakeAsync`, `tick`, `flush` for async operations
- **HTTP Mocking**: Use `HttpTestingController` from `@angular/common/http/testing`
- **Signal Testing**: Access signal values with `signal()` in tests
- **Current Status**: Test infrastructure in place (76/120 passing)

## Module Dependencies

```
common → auth → catalog → inventory → orders → payments
```

Inter-module calls use direct service injection (same JVM), not REST.

## Documentation

- Implementation plans: [docs/plans/](../docs/plans/) (phases 01-16)
- PRD: [docs/prd.md](../docs/prd.md)
- MVP spec: [docs/OrderHub_MVP.md](../docs/OrderHub_MVP.md)
- Swagger UI: `http://localhost:8080/swagger-ui.html` (when backend is running)
- Backend docs: [backend/DOCUMENTATION.md](../backend/DOCUMENTATION.md)
- Frontend docs: [frontend/docs/](../frontend/docs/)

## API Development Guidelines

### OpenAPI/Swagger Documentation

Every controller must include comprehensive Swagger annotations:

```java
@Tag(name = "Products", description = "Product catalog endpoints")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Operation(
        summary = "Get product by ID",
        description = "Retrieves a single product by its unique identifier"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "bearer-jwt")  // For protected endpoints only
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        // implementation
    }
}
```

### Admin Endpoints

Admin endpoints follow a specific pattern:

- Path: `/api/v1/admin/{domain}/*`
- Security: `@PreAuthorize("hasRole('ADMIN')")`
- Separate controller class: `Admin{Domain}Controller`
- Error responses include 403 for non-admin users
- All operations logged with admin context

Example:

```java
@Tag(name = "Admin Products", description = "Product management endpoints (Admin only)")
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
    // implementation
}
```

# Copilot Instructions for Java Logging and Comments

## Java Logging Best Practices

### Logging Framework

- Use SLF4J as the logging facade with a compatible implementation (Logback, Log4j2)
- Always use parameterized logging instead of string concatenation
- Example: `logger.info("User {} logged in at {}", username, timestamp);`

### Log Levels

- **TRACE**: Very detailed information, typically only enabled during development
- **DEBUG**: Detailed information for debugging, disabled in production
- **INFO**: Informational messages about application progress and state
- **WARN**: Potentially harmful situations that should be investigated
- **ERROR**: Error events that might still allow the application to continue
- **FATAL**: Very severe errors that will lead to application abort (Log4j only)

### Logging Guidelines

1. **Never log sensitive data**: passwords, tokens, credit cards, PII
2. **Use appropriate log levels**: Don't use `error` for informational messages
3. **Include context**: User ID, transaction ID, correlation ID when relevant
4. **Avoid logging in loops**: Can cause performance issues and log bloat
5. **Log exceptions properly**: Include the full stack trace
   ```java
   try {
       // risky operation
   } catch (Exception e) {
       logger.error("Failed to process order {}", orderId, e);
   }
   ```
6. **Guard expensive operations**: Use level checks for complex log message creation
   ```java
   if (logger.isDebugEnabled()) {
       logger.debug("Complex data: {}", expensiveToString());
   }
   ```

### What to Log

- **DO LOG**:
  - Application startup/shutdown
  - Configuration changes
  - Significant business events
  - External API calls (request/response)
  - Authentication/authorization events
  - State transitions
  - Performance metrics
  - Recoverable errors

- **DON'T LOG**:
  - Every method entry/exit in production
  - Sensitive data (passwords, tokens, etc.)
  - Full object dumps in production
  - Redundant information already captured elsewhere

## Java Comments Best Practices

### JavaDoc Comments

Use JavaDoc for all public classes, interfaces, methods, and fields:

```java
/**
 * Processes customer orders and updates inventory.
 *
 * @param order the customer order to process, must not be null
 * @param inventory the current inventory state
 * @return the updated order with status
 * @throws OrderProcessingException if the order cannot be fulfilled
 * @throws IllegalArgumentException if order or inventory is null
 * @since 1.2.0
 */
public Order processOrder(Order order, Inventory inventory)
    throws OrderProcessingException {
    // implementation
}
```

### JavaDoc Guidelines

1. **First sentence is critical**: It appears in summary tables, make it concise
2. **Document parameters**: Explain what they are, constraints, and null handling
3. **Document return values**: Explain what is returned and when
4. **Document exceptions**: Explain when and why they are thrown
5. **Include examples**: For complex APIs, show usage examples
6. **Use `@since`**: Tag when adding new public APIs
7. **Use `@deprecated`**: Mark deprecated APIs with migration guidance

### Implementation Comments

```java
// GOOD: Explains WHY, not WHAT
// Using HashMap instead of TreeMap because we don't need sorted keys
// and HashMap provides O(1) lookup vs O(log n)
Map<String, User> userCache = new HashMap<>();

// BAD: Restates the obvious code
// Create a new HashMap
Map<String, User> userCache = new HashMap<>();
```

### Comment Guidelines

1. **Explain WHY, not WHAT**: Code shows what, comments explain why
2. **Document business rules**: Explain the business logic behind decisions
3. **Note workarounds**: Explain temporary fixes and technical debt
   ```java
   // WORKAROUND: Legacy API doesn't handle null, converting to empty string
   // TODO: Remove once we upgrade to v2.0 (ticket: PROJ-1234)
   ```
4. **Warn about gotchas**: Document non-obvious behavior or constraints
5. **Keep comments up-to-date**: Outdated comments are worse than no comments
6. **Avoid commented-out code**: Use version control instead
7. **Use TODO/FIXME/XXX appropriately**:
   - `TODO`: Future enhancement
   - `FIXME`: Known issue that needs fixing
   - `XXX`: Warning about problematic code

### Class-Level Comments

```java
/**
 * Service for managing user authentication and session handling.
 * <p>
 * This service integrates with OAuth 2.0 providers and maintains
 * session state in Redis for horizontal scalability.
 * </p>
 * <p>
 * Thread-safety: This class is thread-safe and can be used as a singleton.
 * </p>
 *
 * @author Development Team
 * @version 2.1.0
 * @since 1.0.0
 */
public class AuthenticationService {
    // implementation
}
```

### Interface Documentation

```java
/**
 * Strategy for caching user data with different eviction policies.
 * <p>
 * Implementations of this interface must be thread-safe.
 * </p>
 *
 * @see LRUCacheStrategy
 * @see TTLCacheStrategy
 */
public interface CacheStrategy {
    // methods
}
```

## Code Examples

### Logger Declaration

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
}
```

### Comprehensive Example

```java
/**
 * Handles user registration with validation and notification.
 *
 * @param request the registration request containing user details
 * @return the created user with generated ID
 * @throws ValidationException if request data is invalid
 * @throws DuplicateUserException if email already exists
 */
public User registerUser(RegistrationRequest request)
    throws ValidationException, DuplicateUserException {

    logger.info("Processing registration for email: {}", request.getEmail());

    // Validate early to fail fast
    if (!isValidEmail(request.getEmail())) {
        logger.warn("Invalid email format rejected: {}", request.getEmail());
        throw new ValidationException("Invalid email format");
    }

    try {
        User user = userRepository.create(request);
        logger.info("User created successfully with ID: {}", user.getId());

        // Send welcome email asynchronously to avoid blocking
        notificationService.sendWelcomeEmail(user);

        return user;

    } catch (DuplicateKeyException e) {
        logger.error("Duplicate email registration attempt: {}",
                     request.getEmail(), e);
        throw new DuplicateUserException("Email already registered", e);
    } catch (Exception e) {
        logger.error("Unexpected error during registration for email: {}",
                     request.getEmail(), e);
        throw new RuntimeException("Registration failed", e);
    }
}
```

## When Generating Code

When GitHub Copilot generates Java code, it should:

- Include appropriate SLF4J logger declarations
- Add JavaDoc for all public methods and classes
- Use parameterized logging
- Log at appropriate levels (INFO for business events, ERROR for exceptions)
- Include inline comments only for complex logic or business rules
- Document all exceptions that can be thrown
- Include null checks and document null handling in JavaDoc
