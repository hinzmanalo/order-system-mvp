# Feature 11: Backend Testing

**Priority**: Quality
**Dependencies**: 06-auth-backend, 07-catalog-backend, 08-inventory-backend, 09-orders-backend, 10-payments-backend
**Parallel with**: 12-frontend-core, 13-frontend-auth-catalog, 14-frontend-cart-orders-payments, 15-frontend-admin
**Blocks**: 16-integration-polish

---

## Overview

Implement comprehensive unit tests (JUnit 5 + Mockito) and integration tests (SpringBootTest + Testcontainers) targeting >80% service-layer coverage. This feature can run entirely in parallel with all frontend work.

## User Stories

- All stories are validated through tests
- US-024: Concurrent order handling — tested via integration test with ExecutorService

## Tasks

### 11.1 Test infrastructure

- [ ] Verify Testcontainers PostgreSQL dependency in `pom.xml` (test scope)
- [ ] Create `com.orderhub.BaseIntegrationTest.java`:
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)`
  - `@Testcontainers`
  - `@Container` static PostgreSQLContainer with postgres:16-alpine
  - `@DynamicPropertySource` to set datasource URL, username, password
  - Shared container instance across all integration tests (singleton pattern)
- [ ] Configure `src/test/resources/application-test.yml` if needed

### 11.2 Auth unit tests

- [ ] `com.orderhub.auth.service.AuthServiceTest.java`:
  - [ ] `register_success` — valid input creates user with hashed password
  - [ ] `register_duplicateEmail_throws` — throws DuplicateResourceException
  - [ ] `login_validCredentials_returnsTokens` — returns TokenResponse with access + refresh tokens
  - [ ] `login_invalidPassword_throws` — throws authentication exception
  - [ ] `login_nonexistentEmail_throws` — throws authentication exception
  - [ ] `refresh_validToken_returnsNewTokens` — rotates tokens
  - [ ] `refresh_expiredToken_throws` — throws exception
  - [ ] `refresh_invalidToken_throws` — throws exception
  - [ ] `updateRole_userToAdmin_success` — role changed
  - [ ] `updateRole_alreadyAdmin_idempotent` — no error

### 11.3 Catalog unit tests

- [ ] `com.orderhub.catalog.service.ProductServiceTest.java`:
  - [ ] `createProduct_success_createsInventory` — product + inventory(qty=0) created
  - [ ] `createProduct_duplicateSku_throws` — throws DuplicateResourceException
  - [ ] `updateProduct_success` — fields updated
  - [ ] `updateProduct_skuConflict_throws` — SKU taken by another product
  - [ ] `updateProduct_notFound_throws` — throws ResourceNotFoundException
  - [ ] `updateProductStatus_deactivate` — active set to false
  - [ ] `getActiveProducts_filtersInactive` — only active products returned
  - [ ] `getActiveProducts_filterByName` — name filter works
  - [ ] `getActiveProducts_filterByPriceRange` — price filter works
  - [ ] `getProductById_notFound_throws` — throws ResourceNotFoundException

### 11.4 Inventory unit tests

- [ ] `com.orderhub.inventory.service.InventoryServiceTest.java`:
  - [ ] `setStock_success` — quantity set to absolute value
  - [ ] `adjustStock_positiveAdjustment` — quantity increases
  - [ ] `adjustStock_negativeAdjustment` — quantity decreases
  - [ ] `adjustStock_belowZero_throws` — throws InsufficientStockException
  - [ ] `decrementStock_sufficientStock` — quantity decremented
  - [ ] `decrementStock_insufficientStock_throws` — throws InsufficientStockException
  - [ ] `restoreStock_success` — quantity increased
  - [ ] `optimisticLock_conflict` — simulate version mismatch, verify exception

### 11.5 Order unit tests

- [ ] `com.orderhub.orders.service.OrderServiceTest.java`:
  - [ ] `createOrder_success_multipleItems` — order CONFIRMED, inventory decremented, prices snapshot
  - [ ] `createOrder_inactiveProduct_throws` — throws InvalidOrderStateException
  - [ ] `createOrder_insufficientStock_throws` — throws InsufficientStockException, no side effects
  - [ ] `createOrder_productNotFound_throws` — throws ResourceNotFoundException
  - [ ] `cancelOrder_confirmed_success` — status CANCELLED, inventory restored
  - [ ] `cancelOrder_paid_throws` — throws InvalidOrderStateException
  - [ ] `cancelOrder_cancelled_throws` — throws InvalidOrderStateException
  - [ ] `cancelOrder_wrongUser_throws` — throws ResourceNotFoundException
  - [ ] `getUserOrders_onlyOwnOrders` — filtered by userId
  - [ ] `updateOrderStatusToPaid_success` — status changes to PAID
  - [ ] `updateOrderStatusToPaid_notConfirmed_throws` — throws InvalidOrderStateException

### 11.6 Payment unit tests

- [ ] `com.orderhub.payments.service.PaymentServiceTest.java`:
  - [ ] `processPayment_success` — payment SUCCESS, order PAID
  - [ ] `processPayment_gatewayFailure` — payment FAILED, order stays CONFIRMED
  - [ ] `processPayment_idempotent_existingSuccess` — returns existing result, no reprocessing
  - [ ] `processPayment_amountMismatch_throws` — throws PaymentAmountMismatchException
  - [ ] `processPayment_notConfirmedOrder_throws` — throws InvalidOrderStateException
  - [ ] `processPayment_orderNotFound_throws` — throws ResourceNotFoundException

### 11.7 Auth integration tests

- [ ] `com.orderhub.auth.controller.AuthControllerIntegrationTest.java`:
  - [ ] `register_login_accessProtectedResource` — full auth flow
  - [ ] `register_duplicateEmail_returns409` — via HTTP
  - [ ] `login_invalidCredentials_returns401` — via HTTP
  - [ ] `accessProtectedEndpoint_noToken_returns401`
  - [ ] `accessAdminEndpoint_asUser_returns403`
  - [ ] `refreshToken_success_rotatesTokens` — old token invalid
  - [ ] `me_returnsCurrentUser`

### 11.8 Catalog integration tests

- [ ] `com.orderhub.catalog.controller.ProductControllerIntegrationTest.java`:
  - [ ] `getProducts_publicAccess_returnsPaginated`
  - [ ] `getProducts_withFilters_filtersCorrectly`
  - [ ] `createProduct_asAdmin_returns201`
  - [ ] `createProduct_asUser_returns403`
  - [ ] `updateProduct_asAdmin_returns200`
  - [ ] `deactivateProduct_excludedFromListing`

### 11.9 Order integration tests

- [ ] `com.orderhub.orders.controller.OrderControllerIntegrationTest.java`:
  - [ ] `createOrder_pay_fullLifecycle` — order CONFIRMED → PAID, inventory decremented
  - [ ] `createOrder_cancel_fullLifecycle` — order CONFIRMED → CANCELLED, inventory restored
  - [ ] `createOrder_insufficientStock_returns409`
  - [ ] `cancelOrder_paid_returns409`
  - [ ] `adminViewAllOrders_success`

### 11.10 Payment integration tests

- [ ] `com.orderhub.payments.controller.PaymentControllerIntegrationTest.java`:
  - [ ] `processPayment_success_orderPaid`
  - [ ] `processPayment_amountMismatch_returns422`
  - [ ] `processPayment_missingIdempotencyKey_returns400`
  - [ ] `processPayment_duplicateKey_returnsExisting`
  - [ ] `processPayment_cancelledOrder_returns409`

### 11.11 Inventory concurrency test

- [ ] `com.orderhub.inventory.repository.InventoryRepositoryTest.java`:
  - [ ] `concurrentDecrement_optimisticLocking` — use ExecutorService with 10 threads decrementing same product → verify no overselling, some requests fail with OptimisticLockingFailureException

## Verification

- [ ] `mvn test` — all unit tests pass
- [ ] `mvn verify` — all integration tests pass (requires Docker for Testcontainers)
- [ ] Service-layer coverage >80% (check via JaCoCo or IDE coverage tool)

## Files Created

```
backend/src/test/java/com/orderhub/
├── BaseIntegrationTest.java
├── auth/
│   ├── service/AuthServiceTest.java
│   └── controller/AuthControllerIntegrationTest.java
├── catalog/
│   ├── service/ProductServiceTest.java
│   └── controller/ProductControllerIntegrationTest.java
├── inventory/
│   ├── service/InventoryServiceTest.java
│   └── repository/InventoryRepositoryTest.java
├── orders/
│   ├── service/OrderServiceTest.java
│   └── controller/OrderControllerIntegrationTest.java
└── payments/
    ├── service/PaymentServiceTest.java
    └── controller/PaymentControllerIntegrationTest.java
```
