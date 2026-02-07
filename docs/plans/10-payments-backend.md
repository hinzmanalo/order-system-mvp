# Feature 10: Payments Backend

**Priority**: Core
**Dependencies**: 09-orders-backend
**Parallel with**: None
**Blocks**: 11-backend-testing, 14-frontend-cart-orders-payments

---

## Overview

Implement payment processing with a Strategy pattern gateway abstraction, mock gateway (90% success), idempotency via unique keys, and order status transition to PAID on success.

## User Stories

- US-013: Pay for order
- US-025: Payment idempotency

## Tasks

### 10.1 Entities

- [ ] `com.orderhub.payments.entity.PaymentStatus.java` — enum: `SUCCESS`, `FAILED`
- [ ] `com.orderhub.payments.entity.Payment.java`:
  - `@Table(name = "payments")`
  - `id` UUID
  - `order` Order, `@ManyToOne(fetch = LAZY)`, `@JoinColumn(name = "order_id")`
  - `amount` BigDecimal
  - `status` PaymentStatus, `@Enumerated(EnumType.STRING)`
  - `idempotencyKey` String, `@Column(unique = true, nullable = false)`
  - `gatewayReference` String (nullable)
  - `createdAt` LocalDateTime

### 10.2 Repository

- [ ] `com.orderhub.payments.repository.PaymentRepository.java`:
  - `Optional<Payment> findByIdempotencyKey(String idempotencyKey)`
  - `List<Payment> findByOrderId(UUID orderId)`

### 10.3 Payment gateway (Strategy pattern)

- [ ] `com.orderhub.payments.gateway.PaymentGateway.java` — interface:
  - `PaymentGatewayResult processPayment(BigDecimal amount, String referenceId)`
- [ ] `com.orderhub.payments.gateway.PaymentGatewayResult.java` — record or class:
  - `boolean success`
  - `String gatewayReference` (null on failure)
- [ ] `com.orderhub.payments.gateway.MockPaymentGateway.java`:
  - `@Component`
  - Uses `Random` or `ThreadLocalRandom`
  - 90% chance: return `PaymentGatewayResult(true, "MOCK-REF-" + UUID.randomUUID())`
  - 10% chance: return `PaymentGatewayResult(false, null)`

### 10.4 DTOs

- [ ] `PaymentRequest.java` — `@NotNull @Positive` amount (BigDecimal)
- [ ] `PaymentResponse.java` — id, orderId, amount, status, gatewayReference, createdAt

### 10.5 Service

- [ ] `com.orderhub.payments.service.PaymentService.java` — interface:
  - `processPayment(UUID orderId, UUID userId, String idempotencyKey, PaymentRequest)` → PaymentResponse
  - `getPaymentsByOrderId(UUID orderId, UUID userId)` → List\<PaymentResponse\>
- [ ] `com.orderhub.payments.service.PaymentServiceImpl.java`:
  - Injects: `PaymentRepository`, `OrderRepository` (or `OrderService`), `PaymentGateway`
  - **processPayment** (`@Transactional`):
    1. **Idempotency check**: `findByIdempotencyKey(idempotencyKey)`
       - If found and status == SUCCESS → return existing PaymentResponse (no reprocessing)
       - If found and status == FAILED → return existing result (client should use new key to retry)
    2. Load order by orderId
    3. Verify order belongs to userId (or skip for admin) → throw `ResourceNotFoundException` if not
    4. Verify order status == CONFIRMED → throw `InvalidOrderStateException` if not
    5. Validate `request.amount == order.totalAmount` → throw `PaymentAmountMismatchException` if mismatch
    6. Call `paymentGateway.processPayment(amount, orderId.toString())`
    7. Create Payment entity:
       - On success: status=SUCCESS, gatewayReference from result
       - On failure: status=FAILED, gatewayReference=null
    8. Save Payment
    9. If SUCCESS → call `orderService.updateOrderStatusToPaid(orderId)`
    10. Return PaymentResponse
  - **getPaymentsByOrderId**: verify order ownership, query payments by orderId

### 10.6 Controller

- [ ] `com.orderhub.payments.controller.PaymentController.java`:
  - `POST /api/v1/orders/{orderId}/payments`:
    - `@RequestHeader("Idempotency-Key") String idempotencyKey` (required)
    - `@Valid @RequestBody PaymentRequest request`
    - Extract userId from SecurityContext
    - Returns PaymentResponse
    - Missing `Idempotency-Key` header → 400
  - `GET /api/v1/orders/{orderId}/payments`:
    - Extract userId from SecurityContext
    - Returns List\<PaymentResponse\>

## Implementation Notes

- **Idempotency**: The unique constraint on `idempotency_key` in the DB is the last line of defense. The service-level check prevents unnecessary gateway calls.
- **Amount validation**: Comparing BigDecimal values — use `compareTo() == 0` not `equals()` (equals checks scale).
- **Failed payments**: The order stays CONFIRMED. The customer can retry with a new idempotency key.
- **Gateway swapping**: To replace MockPaymentGateway with a real one (Stripe, PayPal), implement the `PaymentGateway` interface and swap the `@Component` annotation.

## Verification

- [ ] `POST /api/v1/orders/{orderId}/payments` with correct amount and `Idempotency-Key` header → payment processed
- [ ] Successful payment (status=SUCCESS) → order transitions to PAID
- [ ] Failed payment (status=FAILED) → order remains CONFIRMED
- [ ] Missing `Idempotency-Key` header → 400
- [ ] Amount mismatch → 422
- [ ] Payment on non-CONFIRMED order (e.g., CANCELLED) → 409
- [ ] Payment on already-PAID order → 409
- [ ] Same `Idempotency-Key` on successful payment → returns original result without reprocessing
- [ ] `GET /api/v1/orders/{orderId}/payments` returns payment history for the order
- [ ] User can only see payments for their own orders

## Files Created

```
backend/src/main/java/com/orderhub/payments/
├── controller/
│   └── PaymentController.java
├── dto/
│   ├── PaymentRequest.java
│   └── PaymentResponse.java
├── entity/
│   ├── Payment.java
│   └── PaymentStatus.java
├── gateway/
│   ├── PaymentGateway.java
│   ├── PaymentGatewayResult.java
│   └── MockPaymentGateway.java
├── repository/
│   └── PaymentRepository.java
└── service/
    ├── PaymentService.java
    └── PaymentServiceImpl.java
```
