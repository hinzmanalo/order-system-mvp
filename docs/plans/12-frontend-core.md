# Feature 12: Frontend Core Infrastructure

**Priority**: Frontend
**Dependencies**: 03-angular-scaffolding, 06-auth-backend
**Parallel with**: 11-backend-testing
**Blocks**: 13-frontend-auth-catalog

---

## Overview

Build the Angular frontend foundation: TypeScript models, core services (auth, cart), HTTP interceptor for JWT, route guards, shared UI components (navbar, toast, confirm dialog, pagination), global styles, and app routing configuration.

## User Stories

- US-003 (frontend): Token refresh — interceptor handles transparent refresh
- US-007: Add to cart — CartService with localStorage persistence
- US-008 (partial): Manage cart — CartService signals

## Tasks

### 12.1 TypeScript models

- [ ] `src/app/core/models/user.model.ts`:
  ```typescript
  export interface User { id: string; email: string; firstName: string; lastName: string; role: 'USER' | 'ADMIN'; createdAt: string; }
  export interface LoginRequest { email: string; password: string; }
  export interface RegisterRequest { email: string; password: string; firstName: string; lastName: string; }
  export interface TokenResponse { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; }
  export interface RefreshRequest { refreshToken: string; }
  export interface UpdateRoleRequest { role: string; }
  ```
- [ ] `src/app/core/models/product.model.ts`:
  ```typescript
  export interface Product { id: string; name: string; description: string; price: number; sku: string; active: boolean; createdAt: string; }
  export interface ProductPage { content: Product[]; totalElements: number; totalPages: number; number: number; size: number; first: boolean; last: boolean; }
  ```
- [ ] `src/app/core/models/order.model.ts`:
  ```typescript
  export interface OrderItem { productId: string; productName: string; quantity: number; unitPrice: number; subtotal: number; }
  export interface Order { id: string; userId: string; status: 'CONFIRMED' | 'PAID' | 'CANCELLED'; items: OrderItem[]; totalAmount: number; createdAt: string; }
  export interface OrderItemRequest { productId: string; quantity: number; }
  export interface CreateOrderRequest { items: OrderItemRequest[]; }
  export interface OrderPage { content: Order[]; totalElements: number; totalPages: number; number: number; size: number; first: boolean; last: boolean; }
  ```
- [ ] `src/app/core/models/payment.model.ts`:
  ```typescript
  export interface Payment { id: string; orderId: string; amount: number; status: 'SUCCESS' | 'FAILED'; gatewayReference: string | null; createdAt: string; }
  export interface PaymentRequest { amount: number; }
  ```
- [ ] `src/app/core/models/cart.model.ts`:
  ```typescript
  export interface CartItem { product: Product; quantity: number; }
  ```

### 12.2 Auth service

- [ ] `src/app/core/services/auth.service.ts`:
  - Injectable, providedIn: 'root'
  - **Signals**:
    - `currentUser = signal<User | null>(null)`
    - `isAuthenticated = computed(() => !!this.currentUser())`
    - `isAdmin = computed(() => this.currentUser()?.role === 'ADMIN')`
  - **Private state**: accessToken stored in class property (memory only), refreshToken in localStorage
  - **Methods**:
    - `register(request: RegisterRequest): Observable<User>`
    - `login(request: LoginRequest): Observable<TokenResponse>` — stores tokens, decodes JWT, sets currentUser
    - `logout(): void` — clears tokens, clears currentUser, navigates to /login
    - `refresh(): Observable<TokenResponse>` — calls /auth/refresh, updates stored tokens
    - `getMe(): Observable<User>` — calls /auth/me, updates currentUser signal
    - `getAccessToken(): string | null` — returns in-memory token
    - `isTokenExpired(): boolean` — checks JWT exp claim
  - **Init**: on service construction, check localStorage for refresh token → attempt silent refresh + getMe()

### 12.3 Cart service

- [ ] `src/app/core/services/cart.service.ts`:
  - Injectable, providedIn: 'root'
  - **Signals**:
    - `cartItems = signal<CartItem[]>([])` — initialized from localStorage
    - `cartCount = computed(() => this.cartItems().reduce((sum, item) => sum + item.quantity, 0))`
    - `cartTotal = computed(() => this.cartItems().reduce((sum, item) => sum + item.product.price * item.quantity, 0))`
  - **Methods**:
    - `addToCart(product: Product, quantity: number): void` — add or increment, persist to localStorage
    - `removeFromCart(productId: string): void` — remove item, persist
    - `updateQuantity(productId: string, quantity: number): void` — update quantity, persist
    - `clearCart(): void` — empty cart, clear localStorage
  - **Persistence**: `localStorage.setItem('cart', JSON.stringify(items))` on every mutation

### 12.4 Auth interceptor

- [ ] `src/app/core/interceptors/auth.interceptor.ts`:
  - Functional interceptor: `export const authInterceptor: HttpInterceptorFn`
  - On outgoing request: if `authService.getAccessToken()` exists, clone request with `Authorization: Bearer <token>` header
  - On 401 response:
    1. If not already refreshing → call `authService.refresh()`
    2. Queue concurrent failed requests
    3. On refresh success → retry all queued requests with new token
    4. On refresh failure → call `authService.logout()`, redirect to /login

### 12.5 Route guards

- [ ] `src/app/core/guards/auth.guard.ts`:
  - Functional `CanActivateFn`
  - If `authService.isAuthenticated()` → allow
  - Else → `router.navigate(['/login'])`, return false
- [ ] `src/app/core/guards/admin.guard.ts`:
  - Functional `CanActivateFn`
  - If `authService.isAdmin()` → allow
  - Else → `router.navigate(['/products'])`, return false

### 12.6 App configuration

- [ ] `src/app/app.config.ts`:
  - `provideRouter(routes)`
  - `provideHttpClient(withInterceptors([authInterceptor]))`
  - `provideAnimations()` (for toast/dialog transitions)
- [ ] `src/app/app.routes.ts`:
  ```typescript
  export const routes: Routes = [
    { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
    { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
    { path: 'products', loadComponent: () => import('./features/catalog/product-list/product-list.component').then(m => m.ProductListComponent) },
    { path: 'products/:id', loadComponent: () => import('./features/catalog/product-detail/product-detail.component').then(m => m.ProductDetailComponent) },
    { path: 'cart', loadComponent: () => import('./features/cart/cart.component').then(m => m.CartComponent), canActivate: [authGuard] },
    { path: 'checkout', loadComponent: () => import('./features/checkout/checkout.component').then(m => m.CheckoutComponent), canActivate: [authGuard] },
    { path: 'orders', loadComponent: () => import('./features/orders/order-list/order-list.component').then(m => m.OrderListComponent), canActivate: [authGuard] },
    { path: 'orders/:id', loadComponent: () => import('./features/orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent), canActivate: [authGuard] },
    { path: 'admin', canActivate: [authGuard, adminGuard], children: [
      { path: '', loadComponent: () => import('./features/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent) },
      { path: 'products', loadComponent: () => import('./features/admin/products/product-management.component').then(m => m.ProductManagementComponent) },
      { path: 'inventory', loadComponent: () => import('./features/admin/inventory/inventory-management.component').then(m => m.InventoryManagementComponent) },
      { path: 'users', loadComponent: () => import('./features/admin/users/user-management.component').then(m => m.UserManagementComponent) },
      { path: 'orders', loadComponent: () => import('./features/admin/orders/order-management.component').then(m => m.OrderManagementComponent) },
    ]},
    { path: '', redirectTo: 'products', pathMatch: 'full' },
    { path: '**', redirectTo: 'products' },
  ];
  ```
- [ ] `src/app/app.component.ts`: layout with `<app-navbar>` + `<router-outlet>`

### 12.7 Shared components

- [ ] **Navbar** (`src/app/shared/components/navbar/navbar.component.ts`):
  - Standalone component
  - Injects AuthService and CartService
  - Shows: "OrderHub" logo/link, "Products" link, "Cart (N)" link with badge, "Orders" link
  - Conditional: "Admin" link (if isAdmin), "Login/Register" or "Logout" button
  - Responsive: hamburger menu toggle for mobile
- [ ] **Toast** (`src/app/shared/components/toast/`):
  - `toast.service.ts`: Injectable with signals
    - `show(message: string, type: 'success' | 'error' | 'info'): void`
    - Auto-dismiss after 4 seconds
  - `toast.component.ts`: Standalone, overlays in top-right corner
    - Color-coded: success=green, error=red, info=blue
    - Dismiss on click
- [ ] **Confirm dialog** (`src/app/shared/components/confirm-dialog/`):
  - `confirm-dialog.service.ts`:
    - `confirm(message: string): Observable<boolean>` — returns true/false
  - `confirm-dialog.component.ts`:
    - Modal overlay with message text
    - "Confirm" and "Cancel" buttons
- [ ] **Pagination** (`src/app/shared/components/pagination/pagination.component.ts`):
  - Standalone component
  - Inputs: `currentPage`, `totalPages`, `totalElements`
  - Output: `pageChange` event emitter
  - Shows: Previous/Next buttons, page numbers, "Page X of Y" text

### 12.8 Global styles

- [ ] `src/styles.scss`:
  - CSS custom properties (variables):
    - `--color-primary`, `--color-success`, `--color-danger`, `--color-warning`, `--color-info`
    - `--color-bg`, `--color-text`, `--color-border`
  - Base reset / normalize styles
  - Typography (font-family, sizes)
  - Button variants: `.btn`, `.btn-primary`, `.btn-danger`, `.btn-outline`
  - Form styles: `.form-group`, `.form-control`, `.form-error`
  - Table styles: `.table`, `.table-striped`
  - Status badges: `.badge`, `.badge-confirmed` (blue), `.badge-paid` (green), `.badge-cancelled` (red)
  - Layout utilities: `.container`, `.card`, `.flex`, `.grid`
  - Loading spinner: `.spinner` with CSS animation
  - Responsive breakpoints: mobile (<768px), tablet (768-1024px), desktop (>1024px)

### 12.9 Currency pipe

- [ ] `src/app/shared/pipes/currency-format.pipe.ts`:
  - Standalone pipe
  - Transforms number to formatted currency string (e.g., `$299.99`)

## Verification

- [ ] `ng serve` compiles without errors
- [ ] Navbar renders with correct links
- [ ] Navigating to /cart without login → redirects to /login
- [ ] Navigating to /admin without admin role → redirects to /products
- [ ] Toast service shows and auto-dismisses notifications
- [ ] Cart service persists items across page refresh (localStorage)
- [ ] Routes lazy-load correctly (check network tab for chunk loading)

## Files Created

```
src/app/
├── app.component.ts          (modified)
├── app.config.ts              (modified)
├── app.routes.ts              (modified)
├── core/
│   ├── models/
│   │   ├── user.model.ts
│   │   ├── product.model.ts
│   │   ├── order.model.ts
│   │   ├── payment.model.ts
│   │   └── cart.model.ts
│   ├── services/
│   │   ├── auth.service.ts
│   │   └── cart.service.ts
│   ├── interceptors/
│   │   └── auth.interceptor.ts
│   └── guards/
│       ├── auth.guard.ts
│       └── admin.guard.ts
├── shared/
│   ├── components/
│   │   ├── navbar/navbar.component.ts
│   │   ├── toast/toast.component.ts
│   │   ├── toast/toast.service.ts
│   │   ├── confirm-dialog/confirm-dialog.component.ts
│   │   ├── confirm-dialog/confirm-dialog.service.ts
│   │   └── pagination/pagination.component.ts
│   └── pipes/
│       └── currency-format.pipe.ts
└── styles.scss                (modified)
```
