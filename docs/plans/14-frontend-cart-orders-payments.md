# Feature 14: Frontend Cart, Orders & Payments

**Priority**: Frontend
**Dependencies**: 13-frontend-auth-catalog, 09-orders-backend, 10-payments-backend
**Parallel with**: 15-frontend-admin
**Blocks**: 16-integration-polish

---

## Overview

Implement the customer shopping flow: cart management, checkout (order creation), order history, order detail with payment and cancellation actions. This completes the full customer journey from browsing to payment.

## User Stories

- US-007: Add to cart (cart page view)
- US-008: Manage cart
- US-009: Place order / checkout (frontend)
- US-010: View order history (frontend)
- US-011: View order detail (frontend)
- US-012: Cancel order (frontend)
- US-013: Pay for order (frontend)

## Tasks

### 14.1 Order API service

- [ ] `src/app/core/services/order.service.ts`:
  - Injectable, providedIn: 'root'
  - **Methods**:
    - `createOrder(request: CreateOrderRequest): Observable<Order>`
      - `POST /api/v1/orders`
    - `getMyOrders(params: { page?: number; size?: number; status?: string; createdAfter?: string; createdBefore?: string }): Observable<OrderPage>`
      - `GET /api/v1/orders` with query params
    - `getOrderById(id: string): Observable<Order>`
      - `GET /api/v1/orders/${id}`
    - `cancelOrder(id: string): Observable<Order>`
      - `POST /api/v1/orders/${id}/cancel`

### 14.2 Payment API service

- [ ] `src/app/core/services/payment.service.ts`:
  - Injectable, providedIn: 'root'
  - **Methods**:
    - `processPayment(orderId: string, request: PaymentRequest): Observable<Payment>`
      - `POST /api/v1/orders/${orderId}/payments`
      - Auto-generate `Idempotency-Key` header: `crypto.randomUUID()` (or uuid library)
      - Add custom header to the HTTP request
    - `getPayments(orderId: string): Observable<Payment[]>`
      - `GET /api/v1/orders/${orderId}/payments`

### 14.3 Cart component

- [ ] `src/app/features/cart/cart.component.ts`:
  - Standalone component, protected by authGuard (in routes)
  - **Template**:
    - Page title: "Shopping Cart"
    - **If cart is empty**:
      - "Your cart is empty" message
      - "Browse Products" link → /products
    - **If cart has items** — table:
      - Columns: Product, Unit Price, Quantity, Subtotal, Action
      - Product name (text)
      - Unit price (formatted currency)
      - Quantity: number input (min=1), update on change → `cartService.updateQuantity()`
      - Subtotal: `price * quantity`
      - Remove button (trash icon / "Remove") → `cartService.removeFromCart()`
    - Cart summary:
      - Total items count
      - **Cart Total** (bold, large)
    - "Proceed to Checkout" button → navigate to /checkout
    - "Continue Shopping" link → /products

### 14.4 Checkout component

- [ ] `src/app/features/checkout/checkout.component.ts`:
  - Standalone component, protected by authGuard
  - **State**:
    - `loading = signal(false)`
    - `error = signal<string | null>(null)`
  - **Template**:
    - Page title: "Checkout"
    - **Order Summary** (read-only):
      - Table of cart items: product name, quantity, unit price, subtotal
      - Order total
    - **If cart is empty** → redirect to /cart
    - "Place Order" button (disabled while loading)
    - Loading spinner during order creation
    - Error display (e.g., insufficient stock — show which products failed)
  - **Logic**:
    - On "Place Order":
      1. Build `CreateOrderRequest` from `cartService.cartItems()`
      2. Call `orderService.createOrder(request)`
      3. On success:
         - `cartService.clearCart()`
         - Toast: "Order placed successfully!"
         - Navigate to `/orders/${order.id}`
      4. On error:
         - Display error message (parse RFC 7807 detail field)
         - Do NOT clear cart (user can adjust and retry)

### 14.5 Order list component

- [ ] `src/app/features/orders/order-list/order-list.component.ts`:
  - Standalone component, protected by authGuard
  - **State**:
    - `orders = signal<Order[]>([])`
    - `loading = signal(true)`
    - `currentPage = signal(0)`
    - `totalPages = signal(0)`
    - `statusFilter = signal<string>('')`
  - **Template**:
    - Page title: "My Orders"
    - Filter bar:
      - Status dropdown: All, CONFIRMED, PAID, CANCELLED
    - Orders table:
      - Columns: Order ID (first 8 chars), Status (badge), Items (#), Total, Date, Action
      - Status badges: CONFIRMED=blue, PAID=green, CANCELLED=red
      - Click row → navigate to `/orders/:id`
    - Pagination component
    - Loading spinner
    - Empty state: "No orders found" with link to /products

### 14.6 Order detail component

- [ ] `src/app/features/orders/order-detail/order-detail.component.ts`:
  - Standalone component, protected by authGuard
  - **State**:
    - `order = signal<Order | null>(null)`
    - `payments = signal<Payment[]>([])`
    - `loading = signal(true)`
    - `paymentLoading = signal(false)`
    - `cancelLoading = signal(false)`
  - **Template**:
    - Back link: "← Back to Orders"
    - Order header:
      - Order ID (full UUID)
      - Status badge (color-coded)
      - Created date
    - Line items table:
      - Columns: Product, Quantity, Unit Price, Subtotal
    - Order total (bold)
    - **Actions** (conditional):
      - If status == CONFIRMED:
        - "Pay Now" button → triggers payment flow
        - "Cancel Order" button → triggers confirmation dialog → cancel flow
      - If status == PAID: show "Order has been paid" success message
      - If status == CANCELLED: show "Order was cancelled" info message
    - **Payment History** section (if payments exist):
      - Table: Payment ID (short), Status, Amount, Gateway Ref, Date
    - Loading spinners for each async action
  - **Logic**:
    - On init: load order + payments in parallel
    - "Pay Now":
      1. `paymentService.processPayment(orderId, { amount: order.totalAmount })`
      2. On success (SUCCESS): toast "Payment successful!", reload order (status=PAID)
      3. On success (FAILED): toast "Payment failed. Please try again." (order stays CONFIRMED)
      4. On error: display error message
    - "Cancel Order":
      1. Show confirm dialog: "Are you sure you want to cancel this order?"
      2. On confirm: `orderService.cancelOrder(orderId)`
      3. On success: toast "Order cancelled", reload order (status=CANCELLED)

## Verification

- [ ] Cart page displays items, allows quantity edit and removal
- [ ] Cart total updates in real-time on quantity change
- [ ] Empty cart shows appropriate message
- [ ] Checkout page shows order summary from cart
- [ ] "Place Order" creates order, clears cart, navigates to order detail
- [ ] Insufficient stock on checkout shows clear error without clearing cart
- [ ] Order list page shows user's orders with status badges
- [ ] Status filter works correctly
- [ ] Order detail page shows full order with line items
- [ ] "Pay Now" processes payment, updates order status to PAID on success
- [ ] "Pay Now" shows failure message when gateway fails (order stays CONFIRMED, can retry)
- [ ] "Cancel Order" shows confirm dialog, cancels on confirm, updates to CANCELLED
- [ ] "Cancel Order" not available for PAID orders
- [ ] Payment history section displays on order detail

## Files Created

```
src/app/
├── core/services/
│   ├── order.service.ts
│   └── payment.service.ts
└── features/
    ├── cart/cart.component.ts
    ├── checkout/checkout.component.ts
    └── orders/
        ├── order-list/order-list.component.ts
        └── order-detail/order-detail.component.ts
```
