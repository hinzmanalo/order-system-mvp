# Feature 16: Integration & Polish

**Priority**: Final
**Dependencies**: ALL previous features (01-15)
**Parallel with**: None
**Blocks**: None (final feature)

---

## Overview

Final integration testing, Docker Compose end-to-end validation, Swagger API documentation polish, frontend unit tests, edge case handling, and code cleanup. This is the final quality gate before the MVP is considered complete.

## User Stories

- US-024: Concurrent order handling (edge case verification)
- US-025: Payment idempotency (edge case verification)
- US-026: API documentation (Swagger polish)
- US-027: Health check (Actuator verification)

## Tasks

### 16.1 Docker Compose end-to-end

- [ ] Verify `docker compose build` succeeds for backend image
- [ ] Verify `docker compose up` starts both `db` and `app` services
- [ ] Verify Flyway migrations run on fresh database (no pre-existing data)
- [ ] Verify seed data is present after startup
- [ ] Verify backend is accessible at `http://localhost:8080`
- [ ] Verify frontend dev server proxies to backend correctly
- [ ] Run full customer flow via browser: register → login → browse → cart → checkout → pay
- [ ] Run full admin flow via browser: login → manage products → manage inventory → manage users → manage orders

### 16.2 Swagger / OpenAPI documentation

- [ ] Verify Swagger UI accessible at `/swagger-ui.html` without authentication
- [ ] Add `@Operation` annotations to all controller methods:
  - Summary (short description)
  - Description (detailed)
- [ ] Add `@ApiResponse` annotations for all response codes (200, 201, 400, 401, 403, 404, 409, 422)
- [ ] Verify endpoints are grouped by tags: Auth, Products, Inventory, Orders, Payments, Admin
- [ ] Verify request/response schemas show correct field types and examples
- [ ] Verify `@Schema` annotations on DTOs where needed (descriptions, examples)

### 16.3 Actuator health check

- [ ] `GET /actuator/health` returns `{"status": "UP"}`
- [ ] Database health indicator shows UP (db connectivity verified)
- [ ] Endpoint accessible without authentication
- [ ] Verify `management.endpoints.web.exposure.include` config is correct

### 16.4 Edge case verification

- [ ] **Concurrent orders**: Two users order the last unit of a product simultaneously
  - One succeeds (CONFIRMED), other gets 409 with "Insufficient Stock" message
  - Verify inventory never goes negative
- [ ] **Payment idempotency**: Submit same `Idempotency-Key` twice for same order
  - First request processes normally
  - Second request returns the original payment result without reprocessing
- [ ] **Token expiry**: Let access token expire (wait 15 min or use short-lived test token)
  - Interceptor automatically refreshes and retries the request
  - If refresh token also expired → redirect to login
- [ ] **Deactivated product in cart**: Add product to cart → admin deactivates it → user tries to checkout
  - Order creation fails with clear error about inactive product
  - Cart is NOT cleared (user can remove the invalid item)
- [ ] **Price change between cart and order**: Note price in cart → admin updates price → place order
  - Order captures the NEW price at order time (snapshot)
  - This is expected behavior per PRD

### 16.5 Frontend tests (Jasmine/Karma)

- [ ] **Component tests**:
  - `login.component.spec.ts` — form validation, submit calls authService, navigation on success
  - `register.component.spec.ts` — form validation, submit calls authService, error display
  - `product-list.component.spec.ts` — renders products, pagination, filter changes trigger reload
  - `cart.component.spec.ts` — renders items, quantity update, remove, empty state
  - `checkout.component.spec.ts` — renders summary, submit creates order, error handling
- [ ] **Service tests** (HttpClientTestingModule):
  - `auth.service.spec.ts` — login/register/refresh make correct HTTP calls
  - `product.service.spec.ts` — getProducts/getProductById with correct params
  - `order.service.spec.ts` — createOrder/getMyOrders/cancelOrder
  - `payment.service.spec.ts` — processPayment includes Idempotency-Key header
  - `cart.service.spec.ts` — add/remove/update/clear with localStorage
- [ ] **Guard tests**:
  - `auth.guard.spec.ts` — allows authenticated, redirects unauthenticated
  - `admin.guard.spec.ts` — allows admin, redirects non-admin
- [ ] Verify: `ng test` passes all tests

### 16.6 Code cleanup

- [ ] **Backend**:
  - Remove unused imports across all Java files
  - Consistent code formatting (indentation, braces, line breaks)
  - Add INFO-level log statements in service methods for business events:
    - "User registered: {email}"
    - "Order created: {orderId} for user {userId}"
    - "Payment processed: {paymentId} status={status}"
    - "Order cancelled: {orderId}"
  - Add WARN-level logs for failures:
    - "Insufficient stock for product {sku}: available={qty}, requested={qty}"
    - "Payment failed for order {orderId}"
  - Verify JWT secret is read from environment/config (not hardcoded)
  - Verify no password hashes in any API response
  - Verify no stack traces in production error responses (only in dev profile)
- [ ] **Frontend**:
  - Remove unused imports
  - Consistent formatting
  - Verify no `console.log` statements in production code
  - Verify environment config is correct (apiUrl)

### 16.7 Security review

- [ ] No SQL injection vectors (all queries use parameterized JPA/JPQL)
- [ ] No XSS vectors (Angular auto-escapes by default; verify no `innerHTML` with user data)
- [ ] No exposed secrets in code (JWT secret from env/config only)
- [ ] CORS configured for specific origin only (not `*`)
- [ ] Admin endpoints properly protected by role check
- [ ] Users can only access their own orders/payments
- [ ] Password validation enforced (min length)
- [ ] BCrypt password hashing with appropriate strength

### 16.8 Final verification checklist

- [ ] `docker compose up` → app starts from scratch with single command
- [ ] Seed data present: admin@orderhub.com (ADMIN), user@orderhub.com (USER), 5 products, inventory
- [ ] **Customer flow**: register → login → browse → add to cart → checkout → pay → view PAID order
- [ ] **Customer flow**: place order → cancel → verify inventory restored
- [ ] **Admin flow**: login → create product → set stock → view in catalog
- [ ] **Admin flow**: login → view all orders → cancel a CONFIRMED order
- [ ] **Admin flow**: login → view users → promote user to admin
- [ ] All API endpoints return correct HTTP status codes per PRD spec
- [ ] All error responses follow RFC 7807 Problem Detail format
- [ ] Swagger UI shows complete, accurate API documentation
- [ ] Actuator `/actuator/health` returns UP
- [ ] `mvn test` → all backend unit tests pass
- [ ] `mvn verify` → all backend integration tests pass
- [ ] `ng test` → all frontend tests pass
- [ ] `ng build` → production build succeeds without errors

## Files Modified

This feature primarily modifies existing files (adding annotations, logs, tests) rather than creating new ones.

```
Modified:
  backend/src/main/java/com/orderhub/**/controller/*.java    (Swagger annotations)
  backend/src/main/java/com/orderhub/**/service/*Impl.java   (log statements)
  backend/src/main/java/com/orderhub/**/dto/*.java            (Schema annotations)

Created:
  frontend/src/app/features/auth/login/login.component.spec.ts
  frontend/src/app/features/auth/register/register.component.spec.ts
  frontend/src/app/features/catalog/product-list/product-list.component.spec.ts
  frontend/src/app/features/cart/cart.component.spec.ts
  frontend/src/app/features/checkout/checkout.component.spec.ts
  frontend/src/app/core/services/auth.service.spec.ts
  frontend/src/app/core/services/product.service.spec.ts
  frontend/src/app/core/services/order.service.spec.ts
  frontend/src/app/core/services/payment.service.spec.ts
  frontend/src/app/core/services/cart.service.spec.ts
  frontend/src/app/core/guards/auth.guard.spec.ts
  frontend/src/app/core/guards/admin.guard.spec.ts
```
