# Feature 15: Frontend Admin

**Priority**: Frontend
**Dependencies**: 13-frontend-auth-catalog, 08-inventory-backend
**Parallel with**: 14-frontend-cart-orders-payments
**Blocks**: 16-integration-polish

---

## Overview

Implement the admin dashboard and all admin management pages: product management (CRUD + status toggle), inventory management (set/adjust stock), user management (list + promote), and order management (list all + cancel). All admin pages are protected by both authGuard and adminGuard.

## User Stories

- US-014: Admin create product (frontend)
- US-015: Admin update product (frontend)
- US-016: Admin activate/deactivate product (frontend)
- US-017: Admin set stock (frontend)
- US-018: Admin adjust stock (frontend)
- US-019: Admin view all orders (frontend)
- US-020: Admin cancel any order (frontend)
- US-021: Admin list users (frontend)
- US-022: Admin promote user (frontend)
- US-023: View inventory (frontend)

## Tasks

### 15.1 Admin API service

- [ ] `src/app/core/services/admin.service.ts`:
  - Injectable, providedIn: 'root'
  - **Product management**:
    - `createProduct(request: ProductRequest): Observable<Product>` → `POST /api/v1/admin/products`
    - `updateProduct(id: string, request: ProductRequest): Observable<Product>` → `PUT /api/v1/admin/products/${id}`
    - `updateProductStatus(id: string, active: boolean): Observable<Product>` → `PATCH /api/v1/admin/products/${id}/status`
  - **Inventory management**:
    - `getInventory(params: { page?: number; size?: number }): Observable<InventoryPage>` → `GET /api/v1/admin/inventory`
    - `getInventoryByProductId(productId: string): Observable<InventoryResponse>` → `GET /api/v1/admin/inventory/${productId}`
    - `setStock(productId: string, quantity: number): Observable<InventoryResponse>` → `PUT /api/v1/admin/inventory/${productId}`
    - `adjustStock(productId: string, adjustment: number): Observable<InventoryResponse>` → `PATCH /api/v1/admin/inventory/${productId}/adjust`
  - **User management**:
    - `getUsers(params: { page?: number; size?: number }): Observable<UserPage>` → `GET /api/v1/admin/users`
    - `getUserById(id: string): Observable<User>` → `GET /api/v1/admin/users/${id}`
    - `updateUserRole(id: string, role: string): Observable<User>` → `PUT /api/v1/admin/users/${id}/role`
  - **Order management**:
    - `getAllOrders(params: { page?: number; size?: number; status?: string; createdAfter?: string; createdBefore?: string }): Observable<OrderPage>` → `GET /api/v1/admin/orders`
    - `getOrderById(id: string): Observable<Order>` → `GET /api/v1/admin/orders/${id}`
    - `cancelOrder(id: string): Observable<Order>` → `POST /api/v1/admin/orders/${id}/cancel`

### 15.2 Admin dashboard

- [ ] `src/app/features/admin/dashboard/admin-dashboard.component.ts`:
  - Standalone component
  - **Template**:
    - Page title: "Admin Dashboard"
    - Navigation cards (grid layout, 2x2):
      - "Products" card → /admin/products — "Manage product catalog"
      - "Inventory" card → /admin/inventory — "Manage stock levels"
      - "Users" card → /admin/users — "Manage user accounts"
      - "Orders" card → /admin/orders — "View and manage all orders"

### 15.3 Product management

- [ ] `src/app/features/admin/products/product-management.component.ts`:
  - Standalone component
  - **State**:
    - `products = signal<Product[]>([])`
    - `loading = signal(true)`
    - `showForm = signal(false)`
    - `editingProduct = signal<Product | null>(null)` (null = create mode)
    - `currentPage = signal(0)`, `totalPages = signal(0)`
  - **Template**:
    - Page title: "Product Management"
    - "Add Product" button → opens form in create mode
    - Products table:
      - Columns: Name, SKU, Price, Status (active/inactive badge), Actions
      - Actions: "Edit" button, "Activate"/"Deactivate" toggle button
    - Pagination
    - Loading spinner
  - **Logic**:
    - Load products on init (including inactive — use admin endpoint or fetch all)
    - "Deactivate" → confirm dialog → `adminService.updateProductStatus(id, false)` → reload
    - "Activate" → `adminService.updateProductStatus(id, true)` → reload
    - "Edit" → set editingProduct, show form

- [ ] `src/app/features/admin/products/product-form.component.ts`:
  - Standalone component (inline or modal)
  - **Inputs**: `product: Product | null` (null = create mode)
  - **Outputs**: `saved: EventEmitter<void>`, `cancelled: EventEmitter<void>`
  - **Form** (Reactive):
    - name (required)
    - description (optional, textarea)
    - price (required, positive number)
    - sku (required)
  - **Logic**:
    - Create mode: `adminService.createProduct()` → toast "Product created" → emit saved
    - Edit mode: pre-fill form → `adminService.updateProduct()` → toast "Product updated" → emit saved
    - Validation errors displayed inline
    - Duplicate SKU error from API displayed

### 15.4 Inventory management

- [ ] `src/app/features/admin/inventory/inventory-management.component.ts`:
  - Standalone component
  - **State**:
    - `inventory = signal<InventoryResponse[]>([])`
    - `loading = signal(true)`
    - `currentPage = signal(0)`, `totalPages = signal(0)`
  - **Template**:
    - Page title: "Inventory Management"
    - Inventory table:
      - Columns: Product Name, SKU, Current Quantity, Actions
      - Actions:
        - "Set Stock" button → inline input or modal to set absolute quantity
        - "Adjust" button → inline input or modal to add/subtract delta
    - Pagination
    - Loading spinner
  - **Logic**:
    - "Set Stock": prompt for quantity → `adminService.setStock(productId, qty)` → reload → toast
    - "Adjust": prompt for +/- value → `adminService.adjustStock(productId, delta)` → reload → toast
    - Handle error for negative stock result (409) → toast error

### 15.5 User management

- [ ] `src/app/features/admin/users/user-management.component.ts`:
  - Standalone component
  - **State**:
    - `users = signal<User[]>([])`
    - `loading = signal(true)`
    - `currentPage = signal(0)`, `totalPages = signal(0)`
  - **Template**:
    - Page title: "User Management"
    - Users table:
      - Columns: Email, Name, Role (badge), Registered, Actions
      - Role badges: USER=gray, ADMIN=purple
      - Actions: "Promote to Admin" button (visible only for USER role)
    - Pagination
    - Loading spinner
  - **Logic**:
    - "Promote to Admin" → confirm dialog: "Promote {email} to Admin?" → `adminService.updateUserRole(id, 'ADMIN')` → reload → toast

### 15.6 Order management

- [ ] `src/app/features/admin/orders/order-management.component.ts`:
  - Standalone component
  - **State**:
    - `orders = signal<Order[]>([])`
    - `loading = signal(true)`
    - `currentPage = signal(0)`, `totalPages = signal(0)`
    - `statusFilter = signal('')`
  - **Template**:
    - Page title: "Order Management"
    - Filter bar:
      - Status dropdown: All, CONFIRMED, PAID, CANCELLED
      - Date range inputs: From, To (optional)
    - Orders table:
      - Columns: Order ID (short), User Email, Status (badge), Total, Date, Actions
      - Actions: "Cancel" button (visible only for CONFIRMED status)
      - Click row → show order detail (inline expand or navigate)
    - Pagination
    - Loading spinner
  - **Logic**:
    - "Cancel" → confirm dialog → `adminService.cancelOrder(id)` → reload → toast
    - Filters trigger reload

## Verification

- [ ] Admin dashboard shows 4 navigation cards
- [ ] Non-admin users cannot access /admin/** (redirected)
- [ ] Product management: list all products (including inactive)
- [ ] Product management: create product via form → appears in list
- [ ] Product management: edit product via form → updates in list
- [ ] Product management: deactivate/activate product with confirmation
- [ ] Inventory management: list all products with stock quantities
- [ ] Inventory management: set stock to absolute value
- [ ] Inventory management: adjust stock by +/- delta
- [ ] Inventory management: error shown when adjustment would go below 0
- [ ] User management: list all users with roles
- [ ] User management: promote USER to ADMIN with confirmation
- [ ] Order management: list all orders across all users
- [ ] Order management: filter by status
- [ ] Order management: cancel CONFIRMED order with confirmation
- [ ] All admin actions show toast notifications on success/error

## Files Created

```
src/app/
├── core/services/
│   └── admin.service.ts
└── features/admin/
    ├── dashboard/admin-dashboard.component.ts
    ├── products/
    │   ├── product-management.component.ts
    │   └── product-form.component.ts
    ├── inventory/inventory-management.component.ts
    ├── users/user-management.component.ts
    └── orders/order-management.component.ts
```
