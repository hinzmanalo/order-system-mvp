# Feature 11: Backend Unit Testing

**Priority**: Quality
**Dependencies**: 06-auth-backend, 07-catalog-backend, 08-inventory-backend, 09-orders-backend, 10-payments-backend
**Parallel with**: 12-frontend-core, 13-frontend-auth-catalog, 14-frontend-cart-orders-payments, 15-frontend-admin
**Blocks**: 16-integration-polish

---

## Overview

Implement comprehensive unit tests (JUnit 5 + Mockito) targeting >80% service-layer coverage. These tests verify business logic in isolation using mocked dependencies. This feature can run entirely in parallel with all frontend work.

**Note**: Integration tests (SpringBootTest + Testcontainers) are deferred to Phase 17, to be implemented after frontend completion.

## User Stories

- All user stories are validated through unit tests
- Business logic is tested in isolation with mocked dependencies

## Tasks

### 11.1 Auth unit tests

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

### 11.2 Catalog unit tests

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

### 11.3 Inventory unit tests

- [ ] `com.orderhub.inventory.service.InventoryServiceTest.java`:
  - [ ] `setStock_success` — quantity set to absolute value
  - [ ] `adjustStock_positiveAdjustment` — quantity increases
  - [ ] `adjustStock_negativeAdjustment` — quantity decreases
  - [ ] `adjustStock_belowZero_throws` — throws InsufficientStockException
  - [ ] `decrementStock_sufficientStock` — quantity decremented
  - [ ] `decrementStock_insufficientStock_throws` — throws InsufficientStockException
  - [ ] `restoreStock_success` — quantity increased
  - [ ] `optimisticLock_conflict` — simulate version mismatch, verify exception

### 11.4 Order unit tests

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

### 11.5 Payment unit tests

- [ ] `com.orderhub.payments.service.PaymentServiceTest.java`:
  - [ ] `processPayment_success` — payment SUCCESS, order PAID
  - [ ] `processPayment_gatewayFailure` — payment FAILED, order stays CONFIRMED
  - [ ] `processPayment_idempotent_existingSuccess` — returns existing result, no reprocessing
  - [ ] `processPayment_amountMismatch_throws` — throws PaymentAmountMismatchException
  - [ ] `processPayment_notConfirmedOrder_throws` — throws InvalidOrderStateException
  - [ ] `processPayment_orderNotFound_throws` — throws ResourceNotFoundException

## Verification

- [ ] `mvn clean test` — all unit tests pass (45 tests total)
- [ ] Service-layer coverage >80% (check via JaCoCo or IDE coverage tool)
- [ ] No integration with external systems (all dependencies mocked)

## Files Created

```
backend/src/test/java/com/orderhub/
├── auth/service/AuthServiceTest.java
├── catalog/service/ProductServiceTest.java
├── inventory/service/InventoryServiceTest.java
├── orders/service/OrderServiceTest.java
└── payments/service/PaymentServiceTest.java
```

## Notes

- All tests use Mockito for mocking repositories and dependencies
- Tests verify business logic, exception handling, and edge cases
- Integration tests (with Testcontainers) deferred to Phase 17
