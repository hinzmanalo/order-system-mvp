# Feature 13: Frontend Auth & Catalog

**Priority**: Frontend
**Dependencies**: 12-frontend-core, 07-catalog-backend
**Parallel with**: 11-backend-testing
**Blocks**: 14-frontend-cart-orders-payments, 15-frontend-admin

---

## Overview

Implement the login, registration, product listing, and product detail pages. These are the first user-facing screens and the entry point for all user journeys.

## User Stories

- US-001: User registration (frontend)
- US-002: User login (frontend)
- US-005: Browse products (frontend)
- US-006: View product detail (frontend)
- US-007: Add to cart (frontend — "Add to Cart" button)

## Tasks

### 13.1 Product API service

- [ ] `src/app/core/services/product.service.ts`:
  - Injectable, providedIn: 'root'
  - **Methods**:
    - `getProducts(params: { page?: number; size?: number; name?: string; minPrice?: number; maxPrice?: number; sort?: string }): Observable<ProductPage>`
      - Builds HttpParams from non-null params
      - `GET /api/v1/products`
    - `getProductById(id: string): Observable<Product>`
      - `GET /api/v1/products/${id}`

### 13.2 Login component

- [ ] `src/app/features/auth/login/login.component.ts`:
  - Standalone component with `ReactiveFormsModule` import
  - **Form**: email (required, email format), password (required)
  - **Template**:
    - Centered card layout
    - Form title: "Sign In"
    - Email input with validation error messages
    - Password input with validation error messages
    - "Sign In" submit button (disabled while loading)
    - Loading spinner on submit
    - Error alert for invalid credentials (from API response)
    - Link: "Don't have an account? Register"
  - **Logic**:
    - On submit → `authService.login()` → on success → `router.navigate(['/products'])`
    - On error → display error message via toast or inline

### 13.3 Register component

- [ ] `src/app/features/auth/register/register.component.ts`:
  - Standalone component with `ReactiveFormsModule` import
  - **Form**: email (required, email), password (required, minLength 6), firstName (required), lastName (required)
  - **Template**:
    - Centered card layout
    - Form title: "Create Account"
    - Four form fields with validation error messages
    - "Create Account" submit button (disabled while loading)
    - Loading spinner on submit
    - Error alert for duplicate email
    - Link: "Already have an account? Sign In"
  - **Logic**:
    - On submit → `authService.register()` → on success → toast "Registration successful" → `router.navigate(['/login'])`
    - On error → display error message

### 13.4 Product list component

- [ ] `src/app/features/catalog/product-list/product-list.component.ts`:
  - Standalone component
  - **State** (signals):
    - `products = signal<Product[]>([])`
    - `loading = signal(true)`
    - `currentPage = signal(0)`
    - `totalPages = signal(0)`
    - `totalElements = signal(0)`
    - `searchName = signal('')`
    - `minPrice = signal<number | null>(null)`
    - `maxPrice = signal<number | null>(null)`
    - `sortBy = signal('name,asc')`
  - **Template**:
    - Page title: "Products"
    - Filter bar:
      - Search input (debounced 300ms, updates searchName signal)
      - Min price / Max price inputs
      - Sort dropdown: Name A-Z, Name Z-A, Price Low-High, Price High-Low
      - "Search" / "Clear Filters" buttons
    - Product grid (responsive: 1-3 columns depending on viewport):
      - Each card shows: product name, truncated description, price (formatted), "Add to Cart" button
      - Click card → navigate to `/products/:id`
    - Pagination component at bottom
    - Loading spinner while fetching
    - Empty state: "No products found" message
  - **Logic**:
    - `ngOnInit` → fetch products with current filters
    - On filter change or page change → re-fetch
    - "Add to Cart" button → `cartService.addToCart(product, 1)` → toast "Added to cart"

### 13.5 Product detail component

- [ ] `src/app/features/catalog/product-detail/product-detail.component.ts`:
  - Standalone component
  - **State** (signals):
    - `product = signal<Product | null>(null)`
    - `loading = signal(true)`
    - `quantity = signal(1)`
  - **Template**:
    - Back link: "← Back to Products"
    - Product card:
      - Name (h1)
      - SKU label
      - Price (large, formatted)
      - Description (full text)
      - Quantity selector (number input, min=1)
      - "Add to Cart" button
    - Loading spinner while fetching
    - 404 handling: "Product not found" message
  - **Logic**:
    - Read `:id` from route params → `productService.getProductById(id)`
    - "Add to Cart" → `cartService.addToCart(product, quantity)` → toast → optionally navigate to /cart

## Verification

- [ ] `/login` page renders with form, submits, redirects on success
- [ ] `/login` with bad credentials shows error
- [ ] `/register` page renders with form, submits, redirects to /login on success
- [ ] `/register` with duplicate email shows error
- [ ] `/products` page loads and shows product cards with pagination
- [ ] Search by name filters products in real-time (debounced)
- [ ] Price range filters work
- [ ] Sort dropdown reorders products
- [ ] Clicking product card navigates to `/products/:id`
- [ ] Product detail page shows full product info
- [ ] "Add to Cart" adds item to cart, cart badge in navbar updates
- [ ] Adding same product again increments quantity

## Files Created

```
src/app/
├── core/services/
│   └── product.service.ts
└── features/
    ├── auth/
    │   ├── login/login.component.ts
    │   └── register/register.component.ts
    └── catalog/
        ├── product-list/product-list.component.ts
        └── product-detail/product-detail.component.ts
```
