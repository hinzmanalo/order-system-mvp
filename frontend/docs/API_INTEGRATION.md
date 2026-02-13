# OrderHub Frontend - API Integration Guide

## Overview

This document describes how the Angular frontend integrates with the Spring Boot backend API. It covers HTTP communication, authentication, error handling, and best practices for API interactions.

## Table of Contents

1. [Backend API Overview](#backend-api-overview)
2. [HTTP Client Setup](#http-client-setup)
3. [API Proxy Configuration](#api-proxy-configuration)
4. [Authentication & Authorization](#authentication--authorization)
5. [Service Layer Pattern](#service-layer-pattern)
6. [Error Handling](#error-handling)
7. [Data Models](#data-models)
8. [API Endpoints Reference](#api-endpoints-reference)
9. [Best Practices](#best-practices)

---

## Backend API Overview

### Base URL

- **Development**: `http://localhost:8080/api/v1`
- **Production**: Configured via environment variables

### API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### Response Format

All API responses follow a consistent structure:

**Success Response**:

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Product Name",
  "price": 99.99,
  "inventory": {
    "quantity": 100,
    "reserved": 5
  }
}
```

**Error Response (RFC 7807 Problem Details)**:

```json
{
  "type": "about:blank",
  "title": "Insufficient Inventory",
  "status": 409,
  "detail": "Available quantity (5) is less than requested (10)",
  "instance": "/api/v1/inventory/reserve"
}
```

**Paginated Response**:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

---

## HTTP Client Setup

### Application Configuration

The HTTP client is configured in `app.config.ts`:

```typescript
import { ApplicationConfig } from "@angular/core";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { authInterceptor } from "./core/interceptors/auth.interceptor";

export const appConfig: ApplicationConfig = {
  providers: [provideHttpClient(withInterceptors([authInterceptor]))],
};
```

### HTTP Interceptor Chain

Interceptors process all HTTP requests and responses:

1. **authInterceptor**: Adds JWT token to requests
2. (Future) **errorInterceptor**: Global error handling
3. (Future) **loggingInterceptor**: Request/response logging

---

## API Proxy Configuration

### Proxy Setup

During development, API requests are proxied to avoid CORS issues.

**File**: `proxy.conf.json`

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

### How It Works

```
Frontend Request: http://localhost:4200/api/v1/products
                          ↓
       Proxy Rewrites: http://localhost:8080/api/v1/products
```

### Starting Dev Server with Proxy

```bash
ng serve --proxy-config proxy.conf.json
# Or simply: npm start (configured in package.json)
```

---

## Authentication & Authorization

### Auth Interceptor

Automatically attaches JWT tokens to API requests.

**File**: `core/interceptors/auth.interceptor.ts`

```typescript
import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../services/auth.service";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Skip auth for login/register endpoints
  if (req.url.includes("/auth/login") || req.url.includes("/auth/register")) {
    return next(req);
  }

  // Add Authorization header if token exists
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(req);
};
```

### Token Management

**AuthService** handles token storage:

```typescript
import { Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, tap } from "rxjs";

@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly TOKEN_KEY = "auth_token";
  private readonly USER_KEY = "current_user";

  private token = signal<string | null>(this.getStoredToken());

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>("/api/v1/auth/login", { email, password }).pipe(
      tap((response) => {
        this.setToken(response.token);
        this.setUser(response.user);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.token.set(null);
  }

  getToken(): string | null {
    return this.token();
  }

  isAuthenticated(): boolean {
    return !!this.token();
  }

  private setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.token.set(token);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private setUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }
}
```

### Handling 401 Unauthorized

```typescript
import { HttpInterceptorFn, HttpErrorResponse } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expired or invalid
        authService.logout();
        router.navigate(["/login"], {
          queryParams: { returnUrl: router.url },
        });
      }
      return throwError(() => error);
    }),
  );
};
```

---

## Service Layer Pattern

### Service Structure

All API communication goes through dedicated services in `core/services/`.

**Example**: `product.service.ts`

```typescript
import { Injectable, signal, computed } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, tap, catchError, throwError } from "rxjs";
import { Product, ProductPage, ProductRequest } from "../models/product.model";

@Injectable({ providedIn: "root" })
export class ProductService {
  private readonly API_URL = "/api/v1/products";

  // Local cache/state
  private productsCache = signal<Product[]>([]);
  readonly products$ = computed(() => this.productsCache());

  constructor(private http: HttpClient) {}

  /**
   * Retrieves paginated list of products with optional filters.
   *
   * GET /api/v1/products
   *
   * @param params - Optional query parameters
   * @returns Observable of ProductPage
   */
  getProducts(params?: { page?: number; size?: number; name?: string; minPrice?: number; maxPrice?: number; sort?: string }): Observable<ProductPage> {
    let httpParams = new HttpParams();

    if (params) {
      if (params.page !== undefined) httpParams = httpParams.set("page", params.page);
      if (params.size !== undefined) httpParams = httpParams.set("size", params.size);
      if (params.name) httpParams = httpParams.set("name", params.name);
      if (params.minPrice !== undefined) httpParams = httpParams.set("minPrice", params.minPrice);
      if (params.maxPrice !== undefined) httpParams = httpParams.set("maxPrice", params.maxPrice);
      if (params.sort) httpParams = httpParams.set("sort", params.sort);
    }

    return this.http.get<ProductPage>(this.API_URL, { params: httpParams }).pipe(
      tap((page) => this.productsCache.set(page.content)),
      catchError(this.handleError),
    );
  }

  /**
   * Retrieves a single product by ID.
   *
   * GET /api/v1/products/{id}
   *
   * @param id - Product UUID
   * @returns Observable of Product
   */
  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.API_URL}/${id}`).pipe(catchError(this.handleError));
  }

  /**
   * Creates a new product (Admin only).
   *
   * POST /api/v1/products
   *
   * @param request - Product creation request
   * @returns Observable of created Product
   */
  createProduct(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.API_URL, request).pipe(
      tap((product) => {
        // Update local cache
        this.productsCache.update((products) => [...products, product]);
      }),
      catchError(this.handleError),
    );
  }

  /**
   * Updates an existing product (Admin only).
   *
   * PUT /api/v1/products/{id}
   *
   * @param id - Product UUID
   * @param request - Product update request
   * @returns Observable of updated Product
   */
  updateProduct(id: string, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.API_URL}/${id}`, request).pipe(
      tap((updatedProduct) => {
        // Update local cache
        this.productsCache.update((products) => products.map((p) => (p.id === id ? updatedProduct : p)));
      }),
      catchError(this.handleError),
    );
  }

  /**
   * Deletes a product (Admin only).
   *
   * DELETE /api/v1/products/{id}
   *
   * @param id - Product UUID
   * @returns Observable of void
   */
  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        // Remove from local cache
        this.productsCache.update((products) => products.filter((p) => p.id !== id));
      }),
      catchError(this.handleError),
    );
  }

  /**
   * Handles HTTP errors.
   */
  private handleError(error: any): Observable<never> {
    console.error("ProductService error:", error);

    let errorMessage = "An error occurred";

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = error.error.message;
    } else {
      // Server-side error
      errorMessage = error.error?.detail || error.message;
    }

    return throwError(() => new Error(errorMessage));
  }
}
```

---

## Error Handling

### Backend Error Format

The backend returns RFC 7807 Problem Details:

```typescript
interface ProblemDetail {
  type: string; // "about:blank" or URI reference
  title: string; // Short, human-readable summary
  status: number; // HTTP status code
  detail: string; // Human-readable explanation
  instance: string; // URI reference to occurrence
}
```

### Handling Errors in Components

```typescript
export class ProductListComponent {
  products = signal<Product[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(private productService: ProductService) {}

  loadProducts() {
    this.loading.set(true);
    this.error.set(null);

    this.productService.getProducts().subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        // Extract user-friendly message
        const message = err.error?.detail || err.message || "Failed to load products";
        this.error.set(message);
        this.loading.set(false);
        console.error("Load products failed:", err);
      },
    });
  }
}
```

### Displaying Errors in Template

```html
<div class="product-list">
  @if (loading()) {
  <app-loading-spinner />
  } @if (error()) {
  <div class="error-message" role="alert">
    <span class="error-icon">⚠️</span>
    {{ error() }}
    <button (click)="loadProducts()">Retry</button>
  </div>
  } @if (!loading() && !error() && products().length === 0) {
  <div class="empty-state">No products found.</div>
  }

  <div class="products-grid">
    @for (product of products(); track product.id) {
    <app-product-card [product]="product" />
    }
  </div>
</div>
```

---

## Data Models

### TypeScript Interfaces

Define interfaces that match backend DTOs.

**File**: `core/models/product.model.ts`

```typescript
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  imageUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  category: string;
  imageUrl?: string;
}

export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

**File**: `core/models/order.model.ts`

```typescript
export interface Order {
  id: string;
  userId: string;
  items: OrderItem[];
  totalAmount: number;
  status: OrderStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  price: number;
  subtotal: number;
}

export enum OrderStatus {
  CONFIRMED = "CONFIRMED",
  PAID = "PAID",
  CANCELLED = "CANCELLED",
}

export interface OrderRequest {
  items: {
    productId: string;
    quantity: number;
  }[];
}
```

---

## API Endpoints Reference

### Authentication

| Method | Endpoint                | Description             |
| ------ | ----------------------- | ----------------------- |
| POST   | `/api/v1/auth/register` | Register new user       |
| POST   | `/api/v1/auth/login`    | Login and get JWT token |

### Products

| Method | Endpoint                | Description               | Auth Required |
| ------ | ----------------------- | ------------------------- | ------------- |
| GET    | `/api/v1/products`      | List products (paginated) | No            |
| GET    | `/api/v1/products/{id}` | Get product by ID         | No            |
| POST   | `/api/v1/products`      | Create product            | Admin         |
| PUT    | `/api/v1/products/{id}` | Update product            | Admin         |
| DELETE | `/api/v1/products/{id}` | Delete product            | Admin         |

### Inventory

| Method | Endpoint                        | Description               | Auth Required |
| ------ | ------------------------------- | ------------------------- | ------------- |
| GET    | `/api/v1/inventory/{productId}` | Get inventory for product | No            |
| POST   | `/api/v1/inventory/reserve`     | Reserve inventory         | Yes           |
| POST   | `/api/v1/inventory/release`     | Release inventory         | Yes           |
| PUT    | `/api/v1/inventory/{productId}` | Update inventory          | Admin         |

### Orders

| Method | Endpoint                     | Description      | Auth Required |
| ------ | ---------------------------- | ---------------- | ------------- |
| GET    | `/api/v1/orders`             | List user orders | Yes           |
| GET    | `/api/v1/orders/{id}`        | Get order by ID  | Yes           |
| POST   | `/api/v1/orders`             | Create order     | Yes           |
| POST   | `/api/v1/orders/{id}/cancel` | Cancel order     | Yes           |

### Payments

| Method | Endpoint                | Description       | Auth Required |
| ------ | ----------------------- | ----------------- | ------------- |
| POST   | `/api/v1/payments`      | Process payment   | Yes           |
| GET    | `/api/v1/payments/{id}` | Get payment by ID | Yes           |

---

## Best Practices

### 1. Use Typed HTTP Calls

Always specify response types:

```typescript
// Good ✅
this.http.get<Product[]>("/api/v1/products");

// Bad ❌
this.http.get("/api/v1/products");
```

### 2. Handle Loading States

```typescript
loading = signal(false);

loadData() {
  this.loading.set(true);
  this.service.getData().subscribe({
    next: (data) => {
      this.data.set(data);
      this.loading.set(false);
    },
    error: (err) => {
      this.loading.set(false);
      // handle error
    }
  });
}
```

### 3. Unsubscribe from Observables

Use `async` pipe in templates to auto-unsubscribe:

```typescript
// Component
products$ = this.productService.getProducts();

// Template
<div *ngFor="let product of products$ | async">
```

Or use `takeUntilDestroyed()` in constructor:

```typescript
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

constructor() {
  this.productService.getProducts()
    .pipe(takeUntilDestroyed())
    .subscribe(products => this.products.set(products));
}
```

### 4. Cache API Responses

For data that doesn't change often:

```typescript
private cache = new Map<string, Product>();

getProduct(id: string): Observable<Product> {
  const cached = this.cache.get(id);
  if (cached) {
    return of(cached);
  }

  return this.http.get<Product>(`/api/v1/products/${id}`).pipe(
    tap(product => this.cache.set(id, product))
  );
}
```

### 5. Use Environment Variables

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};

// service
import { environment } from '../../environments/environment';

private readonly API_URL = `${environment.apiUrl}/products`;
```

### 6. Implement Retry Logic

For transient failures:

```typescript
import { retry, catchError } from "rxjs/operators";

this.http.get<Product[]>("/api/v1/products").pipe(
  retry(3), // Retry up to 3 times
  catchError(this.handleError),
);
```

### 7. Request Cancellation

Cancel in-flight requests on navigation:

```typescript
import { Subject, takeUntil } from 'rxjs';

private destroy$ = new Subject<void>();

ngOnInit() {
  this.loadProducts();
}

ngOnDestroy() {
  this.destroy$.next();
  this.destroy$.complete();
}

loadProducts() {
  this.productService.getProducts()
    .pipe(takeUntil(this.destroy$))
    .subscribe(products => this.products.set(products));
}
```

### 8. Idempotency for Critical Operations

For payments and other critical operations:

```typescript
processPayment(request: PaymentRequest): Observable<Payment> {
  const idempotencyKey = this.generateIdempotencyKey();

  return this.http.post<Payment>('/api/v1/payments', request, {
    headers: { 'Idempotency-Key': idempotencyKey }
  });
}

private generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36)}`;
}
```

---

## Testing API Integration

### Mocking HTTP Calls

```typescript
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";

describe("ProductService", () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductService],
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // No outstanding requests
  });

  it("should fetch products", () => {
    const mockProducts: Product[] = [{ id: "1", name: "Product 1", price: 99.99 }];

    service.getProducts().subscribe((page) => {
      expect(page.content).toEqual(mockProducts);
    });

    const req = httpMock.expectOne("/api/v1/products");
    expect(req.request.method).toBe("GET");
    req.flush({ content: mockProducts, page: 0, size: 20 });
  });
});
```

---

**For more information, see the [Backend API Documentation](../../backend/docs/API_QUICK_REFERENCE.md) 😊**
