# Feature 17: Backend Integration Testing

**Priority**: Quality (Post-MVP)
**Dependencies**: 16-integration-polish (frontend & backend complete)
**Parallel with**: None
**Blocks**: Production deployment

---

## Overview

Implement comprehensive integration tests using SpringBootTest with Testcontainers to verify end-to-end functionality across all backend modules. These tests validate the full request-response lifecycle including database interactions, security, and inter-module communication.

**Note**: This phase was deferred until after frontend completion to focus on delivering the core MVP. Unit tests (Phase 11) provide adequate coverage for business logic during development.

## User Stories

- All user stories are validated through integration tests
- US-024: Concurrent order handling — tested via integration test with ExecutorService
- Full request-response cycles tested including HTTP status codes, headers, and JSON serialization

## Tasks

### 17.1 Test Infrastructure

- [ ] Add Testcontainers dependencies to `pom.xml`:
  - `org.testcontainers:testcontainers:1.21.4` (test scope)
  - `org.testcontainers:postgresql:1.21.4` (test scope)
  - `org.testcontainers:junit-jupiter:1.21.4` (test scope)
- [ ] Add testcontainers-bom to `<dependencyManagement>`
- [ ] Create `com.orderhub.BaseIntegrationTest.java`:
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)`
  - `@Testcontainers`
  - `@Container` static PostgreSQLContainer with postgres:16-alpine
  - `@DynamicPropertySource` to set datasource URL, username, password
  - Shared container instance across all integration tests (singleton pattern)
  - Expose `@Autowired TestRestTemplate` or `WebTestClient`
- [ ] Create `src/test/resources/application-test.yml`:
  - Disable Flyway: `spring.flyway.enabled=false`
  - Use `spring.jpa.hibernate.ddl-auto=create-drop`
  - Point to test database: `jdbc:postgresql://localhost:5432/orderhub_test`
- [ ] Configure maven-failsafe-plugin in `pom.xml`:
  - Include patterns: `**/*IT.java`, `**/*IntegrationTest.java`
  - Configure goals: `integration-test`, `verify`
- [ ] Configure maven-surefire-plugin to exclude integration tests:
  - Exclude patterns: `**/*IT.java`, `**/*IntegrationTest.java`, `**/BaseIntegrationTest.java`

### 17.2 Auth Integration Tests

- [ ] `com.orderhub.auth.controller.AuthControllerIntegrationTest.java`:
  - [ ] `register_validRequest_returns201` — POST /api/v1/auth/register
  - [ ] `register_duplicateEmail_returns409` — verify HTTP 409 + error message
  - [ ] `login_validCredentials_returns200WithTokens` — POST /api/v1/auth/login
  - [ ] `login_invalidPassword_returns401` — verify HTTP 401
  - [ ] `login_nonexistentEmail_returns401` — verify HTTP 401
  - [ ] `refresh_validToken_returns200WithNewTokens` — POST /api/v1/auth/refresh
  - [ ] `refresh_expiredToken_returns401` — verify token expiration handling
  - [ ] `me_validToken_returns200WithUser` — GET /api/v1/auth/me
  - [ ] `me_noToken_returns401` — verify authentication required
  - [ ] `accessProtectedEndpoint_noToken_returns401` — generic protected endpoint
  - [ ] `accessAdminEndpoint_asUser_returns403` — verify role-based access control
  - [ ] `fullAuthFlow_registerLoginRefreshAccess` — complete auth lifecycle

### 17.3 Catalog Integration Tests

- [ ] `com.orderhub.catalog.controller.ProductControllerIntegrationTest.java`:
  - [ ] `getProducts_publicAccess_returns200Paginated` — GET /api/v1/products (no auth)
  - [ ] `getProducts_withNameFilter_filtersCorrectly` — query param: name
  - [ ] `getProducts_withPriceRange_filtersCorrectly` — query params: minPrice, maxPrice
  - [ ] `getProducts_withPagination_returnsCorrectPage` — query params: page, size
  - [ ] `getProductById_exists_returns200` — GET /api/v1/products/{id}
  - [ ] `getProductById_notFound_returns404` — verify 404 response
  - [ ] `createProduct_asAdmin_returns201` — POST /api/v1/products (admin token)
  - [ ] `createProduct_asUser_returns403` — verify admin-only access
  - [ ] `createProduct_duplicateSku_returns409` — verify SKU uniqueness
  - [ ] `createProduct_invalidData_returns400` — verify validation errors
  - [ ] `updateProduct_asAdmin_returns200` — PUT /api/v1/products/{id}
  - [ ] `updateProduct_asUser_returns403` — verify admin-only access
  - [ ] `updateProduct_notFound_returns404` — verify 404 response
  - [ ] `deactivateProduct_asAdmin_excludedFromPublicListing` — verify active filter

### 17.4 Order Integration Tests

- [ ] `com.orderhub.orders.controller.OrderControllerIntegrationTest.java`:
  - [ ] `createOrder_validRequest_returns201ConfirmedOrder` — POST /api/v1/orders
  - [ ] `createOrder_multipleItems_decrementInventory` — verify inventory changes in DB
  - [ ] `createOrder_insufficientStock_returns409` — verify HTTP 409 + error
  - [ ] `createOrder_inactiveProduct_returns400` — verify validation
  - [ ] `createOrder_productNotFound_returns404` — verify 404 response
  - [ ] `createOrder_unauthorized_returns401` — verify auth required
  - [ ] `getUserOrders_asUser_returns200OnlyOwnOrders` — GET /api/v1/orders
  - [ ] `getUserOrders_asAdmin_returns200AllOrders` — verify admin sees all
  - [ ] `getOrderById_ownOrder_returns200` — GET /api/v1/orders/{id}
  - [ ] `getOrderById_otherUsersOrder_returns404` — verify user isolation
  - [ ] `getOrderById_asAdmin_returns200AnyOrder` — verify admin access
  - [ ] `cancelOrder_confirmed_returns200AndRestoresInventory` — POST /api/v1/orders/{id}/cancel
  - [ ] `cancelOrder_paid_returns409` — verify state validation
  - [ ] `cancelOrder_cancelled_returns409` — verify idempotency
  - [ ] `fullOrderLifecycle_createPayCancel` — CONFIRMED → PAID → verify cancel blocked

### 17.5 Payment Integration Tests

- [ ] `com.orderhub.payments.controller.PaymentControllerIntegrationTest.java`:
  - [ ] `processPayment_validRequest_returns200AndMarksOrderPaid` — POST /api/v1/payments/process
  - [ ] `processPayment_verifyIdempotencyKeyRequired_returns400` — missing header
  - [ ] `processPayment_duplicateIdempotencyKey_returns200SameResult` — verify idempotency
  - [ ] `processPayment_amountMismatch_returns422` — payment amount ≠ order total
  - [ ] `processPayment_orderNotFound_returns404` — invalid orderId
  - [ ] `processPayment_orderNotConfirmed_returns409` — verify state validation
  - [ ] `processPayment_orderAlreadyPaid_returns409` — verify state validation
  - [ ] `processPayment_gatewayFailure_returns422AndOrderStaysConfirmed` — verify failure handling
  - [ ] `processPayment_unauthorized_returns401` — verify auth required
  - [ ] `fullPaymentFlow_orderPaymentVerification` — create order → pay → verify status

### 17.6 Inventory Concurrency Test

- [ ] `com.orderhub.inventory.repository.InventoryRepositoryTest.java`:
  - [ ] `concurrentDecrement_optimisticLocking_preventsOverselling`:
    - Create product with inventory quantity = 10
    - Use ExecutorService with 15 threads attempting to decrement by 1
    - Expect 10 successful decrements, 5 failures with `OptimisticLockingFailureException`
    - Verify final inventory quantity = 0 (no overselling)
    - Verify database version column incremented correctly

### 17.7 Cross-Module Integration Tests

- [ ] `com.orderhub.e2e.E2EWorkflowIntegrationTest.java`:
  - [ ] `completeUserJourney_registerBrowseOrderPay`:
    1. Register new user
    2. Login and get JWT
    3. Browse products (public)
    4. Create order (2 items)
    5. Verify inventory decremented
    6. Process payment with valid idempotency key
    7. Verify order status = PAID
    8. Verify payment record created
  - [ ] `adminWorkflow_createProductUpdateInventoryViewOrders`:
    1. Register admin user (or update existing to ADMIN role)
    2. Create new product
    3. Update inventory stock
    4. View all orders across all users
  - [ ] `orderCancellationFlow_verifyInventoryRestoration`:
    1. Create order (decrement inventory)
    2. Cancel order
    3. Verify inventory restored to original quantity
    4. Attempt payment on cancelled order → expect 409

## Verification

- [ ] `mvn verify` — all integration tests pass (requires Docker for Testcontainers)
- [ ] Integration tests run in CI/CD pipeline
- [ ] All HTTP status codes verified (200, 201, 400, 401, 403, 404, 409, 422)
- [ ] Database state verified after each operation
- [ ] Testcontainers cleanup verified (no orphaned containers)

## Files Created

```
backend/src/test/java/com/orderhub/
├── BaseIntegrationTest.java
├── auth/controller/AuthControllerIntegrationTest.java
├── catalog/controller/ProductControllerIntegrationTest.java
├── orders/controller/OrderControllerIntegrationTest.java
├── payments/controller/PaymentControllerIntegrationTest.java
├── inventory/repository/InventoryRepositoryTest.java
└── e2e/E2EWorkflowIntegrationTest.java

backend/src/test/resources/
└── application-test.yml
```

## Technical Notes

### Testcontainers Configuration

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("orderhub_test")
    .withUsername("orderhub")
    .withPassword("orderhub");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

### Maven Failsafe Plugin Configuration

```xml
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <includes>
            <include>**/*IT.java</include>
            <include>**/*IntegrationTest.java</include>
        </includes>
    </configuration>
</plugin>
```

### Test Naming Convention

- Integration tests: `*IntegrationTest.java` or `*IT.java`
- Unit tests: `*Test.java` (but not matching IT patterns)
- Surefire runs `*Test.java` excluding IT patterns (`mvn test`)
- Failsafe runs `*IntegrationTest.java` and `*IT.java` (`mvn verify`)

### Performance Considerations

- Testcontainers startup time: ~5-10s (singleton container pattern mitigates)
- Consider parallel test execution if suite grows large
- Use `@Sql` scripts for complex data setup if needed

## Success Criteria

- [ ] All integration tests pass consistently
- [ ] No flaky tests (run suite 10x, all pass)
- [ ] Docker Desktop running locally (or Testcontainers compatible environment)
- [ ] Maven `verify` phase completes successfully
- [ ] Integration tests catch real bugs (e.g., serialization issues, transaction boundaries)

## Future Enhancements (Post Phase 17)

- Add performance/load tests with JMeter or Gatling
- Add contract testing (Spring Cloud Contract or Pact)
- Add chaos engineering tests (Testcontainers Toxiproxy)
- Configure test coverage reports with JaCoCo (combined unit + integration)
