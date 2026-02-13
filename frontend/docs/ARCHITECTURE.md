# OrderHub Frontend Architecture

## Overview

The OrderHub frontend is built with **Angular 17+** using a modern, standalone component architecture. It follows Angular best practices with a clear separation of concerns, reactive state management using Signals, and lazy-loaded feature modules.

## Architecture Style

**Feature-Based Modular Architecture**

- Standalone components (no NgModules)
- Feature-based directory structure
- Lazy-loaded routes for optimal performance
- Reactive programming with RxJS and Angular Signals
- Functional guards and interceptors

## Tech Stack

| Technology      | Version | Purpose                   |
| --------------- | ------- | ------------------------- |
| Angular         | 17.3+   | Core framework            |
| TypeScript      | 5.4+    | Type-safe development     |
| RxJS            | 7.8+    | Reactive programming      |
| Angular Signals | 17.3+   | Reactive state management |
| SCSS            | -       | Styling and theming       |
| Karma + Jasmine | 5.1+    | Unit testing              |

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Browser (Port 4200)                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Angular Application                      │   │
│  │                                                       │   │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │   │
│  │  │  Features  │  │   Shared   │  │    Core    │    │   │
│  │  └────────────┘  └────────────┘  └────────────┘    │   │
│  │                                                       │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │         Angular Router (Lazy Loading)         │   │   │
│  │  └──────────────────────────────────────────────┘   │   │
│  │                                                       │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │    HTTP Client + Interceptors                 │   │   │
│  │  └──────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ /api/* (Proxied)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Backend API (Port 8080)                         │
│                  Spring Boot REST API                         │
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/                    # Singleton services and app-wide utilities
│   │   │   ├── guards/              # Route guards (auth, admin)
│   │   │   │   ├── auth.guard.ts
│   │   │   │   └── admin.guard.ts
│   │   │   ├── interceptors/        # HTTP interceptors
│   │   │   │   └── auth.interceptor.ts
│   │   │   ├── models/              # Core TypeScript interfaces/types
│   │   │   │   ├── user.model.ts
│   │   │   │   ├── product.model.ts
│   │   │   │   ├── order.model.ts
│   │   │   │   └── ...
│   │   │   └── services/            # Singleton services (HTTP, state)
│   │   │       ├── auth.service.ts
│   │   │       ├── cart.service.ts
│   │   │       └── product.service.ts
│   │   │
│   │   ├── shared/                  # Reusable components, pipes, directives
│   │   │   ├── components/          # Shared UI components
│   │   │   │   ├── navbar/
│   │   │   │   ├── footer/
│   │   │   │   ├── loading-spinner/
│   │   │   │   └── ...
│   │   │   └── pipes/               # Custom pipes
│   │   │       ├── currency.pipe.ts
│   │   │       └── ...
│   │   │
│   │   ├── features/                # Feature modules (lazy-loaded)
│   │   │   ├── auth/                # Authentication feature
│   │   │   │   ├── login/
│   │   │   │   └── register/
│   │   │   ├── catalog/             # Product catalog
│   │   │   │   ├── product-list/
│   │   │   │   └── product-detail/
│   │   │   ├── cart/                # Shopping cart
│   │   │   ├── checkout/            # Checkout process
│   │   │   ├── orders/              # Order management
│   │   │   │   ├── order-list/
│   │   │   │   └── order-detail/
│   │   │   └── admin/               # Admin panel
│   │   │       ├── dashboard/
│   │   │       ├── products/
│   │   │       ├── inventory/
│   │   │       ├── orders/
│   │   │       └── users/
│   │   │
│   │   ├── app.component.ts         # Root component
│   │   ├── app.config.ts            # App-level providers
│   │   └── app.routes.ts            # Route configuration
│   │
│   ├── assets/                      # Static files (images, icons)
│   ├── environments/                # Environment configurations
│   └── styles.scss                  # Global styles
│
├── docs/                            # Documentation
├── angular.json                     # Angular workspace config
├── package.json                     # Dependencies
├── proxy.conf.json                  # Dev server API proxy
└── tsconfig.json                    # TypeScript config
```

## Core Architectural Patterns

### 1. Standalone Components

All components are standalone, eliminating the need for NgModules:

```typescript
@Component({
  selector: "app-product-list",
  standalone: true,
  imports: [CommonModule, RouterModule, ProductCardComponent],
  templateUrl: "./product-list.component.html",
})
export class ProductListComponent {}
```

**Benefits**:

- Simplified dependency management
- Better tree-shaking
- Easier to reason about dependencies
- Faster compilation

### 2. Lazy Loading

Features are lazy-loaded via route configuration:

```typescript
{
  path: 'admin',
  loadComponent: () =>
    import('./features/admin/dashboard/admin-dashboard.component').then(
      (m) => m.AdminDashboardComponent
    ),
  canActivate: [authGuard, adminGuard],
}
```

**Benefits**:

- Reduced initial bundle size
- Faster first contentful paint
- Load features on-demand

### 3. Reactive State Management

#### Angular Signals (Primary)

Used for local component state and synchronous values:

```typescript
export class CartService {
  private cartItems = signal<CartItem[]>([]);

  // Computed values automatically update
  readonly totalItems = computed(() => this.cartItems().reduce((sum, item) => sum + item.quantity, 0));

  readonly totalPrice = computed(() => this.cartItems().reduce((sum, item) => sum + item.price * item.quantity, 0));

  addItem(item: CartItem) {
    this.cartItems.update((items) => [...items, item]);
  }
}
```

#### RxJS Observables (Secondary)

Used for asynchronous operations (HTTP, events):

```typescript
export class ProductService {
  constructor(private http: HttpClient) {}

  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>("/api/v1/products");
  }
}
```

**Pattern**: Signals for state, Observables for streams.

### 4. Functional Guards

Guards use functional approach (CanActivateFn):

```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(["/login"], {
    queryParams: { returnUrl: state.url },
  });
};
```

### 5. HTTP Interceptors

Functional interceptors for cross-cutting concerns:

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req);
};
```

### 6. Smart vs. Presentational Components

**Smart (Container) Components**:

- Manage state and business logic
- Inject services
- Handle routing and data fetching
- Example: `ProductListComponent`

**Presentational (Dumb) Components**:

- Pure UI rendering
- Receive data via `@Input()`
- Emit events via `@Output()`
- Reusable and testable
- Example: `ProductCardComponent`

```typescript
// Presentational Component
@Component({
  selector: "app-product-card",
  standalone: true,
  template: `...`,
})
export class ProductCardComponent {
  @Input({ required: true }) product!: Product;
  @Output() addToCart = new EventEmitter<Product>();
}

// Smart Component
@Component({
  selector: "app-product-list",
  standalone: true,
  imports: [ProductCardComponent],
  template: ` <app-product-card *ngFor="let product of products()" [product]="product" (addToCart)="handleAddToCart($event)" /> `,
})
export class ProductListComponent {
  products = signal<Product[]>([]);

  constructor(private productService: ProductService) {
    this.loadProducts();
  }

  private loadProducts() {
    this.productService.getProducts().subscribe((products) => this.products.set(products));
  }

  handleAddToCart(product: Product) {
    this.cartService.addItem(product);
  }
}
```

## Data Flow

### Request Flow (User → Backend)

```
┌──────────────┐
│   Component  │  User clicks "Add to Cart"
└──────┬───────┘
       │
       │ Calls service method
       ▼
┌──────────────┐
│   Service    │  cartService.addItem(product)
└──────┬───────┘
       │
       │ Makes HTTP request
       ▼
┌──────────────┐
│ Interceptor  │  Adds auth token
└──────┬───────┘
       │
       │ HTTP Client
       ▼
┌──────────────┐
│   Proxy      │  /api/* → http://localhost:8080/api/*
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   Backend    │  Spring Boot processes request
└──────────────┘
```

### Response Flow (Backend → User)

```
┌──────────────┐
│   Backend    │  Returns JSON response
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   Proxy      │  Forwards response
└──────┬───────┘
       │
       │ HTTP response
       ▼
┌──────────────┐
│ Interceptor  │  Error handling, logging
└──────┬───────┘
       │
       │ Observable emits
       ▼
┌──────────────┐
│   Service    │  Updates signal/state
└──────┬───────┘
       │
       │ Change detection
       ▼
┌──────────────┐
│   Component  │  UI updates automatically
└──────────────┘
```

## State Management Strategy

### Local Component State

Use Angular Signals for component-specific state.

### Cross-Component State

Use singleton services with Signals for shared state:

```typescript
@Injectable({ providedIn: "root" })
export class CartService {
  private items = signal<CartItem[]>([]);

  readonly items$ = computed(() => this.items());
  readonly count = computed(() => this.items().length);
}
```

### Server State

Use RxJS Observables with services, cache when appropriate:

```typescript
@Injectable({ providedIn: "root" })
export class ProductService {
  private cache = new Map<string, Product>();

  getProduct(id: string): Observable<Product> {
    const cached = this.cache.get(id);
    if (cached) {
      return of(cached);
    }

    return this.http.get<Product>(`/api/v1/products/${id}`).pipe(tap((product) => this.cache.set(id, product)));
  }
}
```

## Routing Strategy

### Route Guards

1. **authGuard**: Protects routes requiring authentication
2. **adminGuard**: Protects admin-only routes

Guards run in order: `canActivate: [authGuard, adminGuard]`

### Lazy Loading Strategy

All feature routes are lazy-loaded to optimize bundle size:

- Initial bundle: Core + Shared + App shell
- Feature bundles: Loaded on-demand when routes are accessed

### Route Parameters

```typescript
// URL: /products/123e4567-e89b-12d3-a456-426614174000
this.route.paramMap.subscribe((params) => {
  const id = params.get("id");
  this.loadProduct(id);
});
```

## Security Architecture

### Authentication Flow

```
┌──────────┐       ┌──────────┐       ┌──────────┐
│  Login   │──────▶│   Auth   │──────▶│ Backend  │
│Component │       │ Service  │       │   API    │
└──────────┘       └──────────┘       └──────────┘
                         │
                         │ Store JWT token
                         ▼
                   localStorage
                         │
                         │ Attach to requests
                         ▼
                   ┌──────────┐
                   │   Auth   │
                   │Interceptor│
                   └──────────┘
```

### Token Management

- JWT tokens stored in `localStorage`
- Tokens attached to API requests via `authInterceptor`
- Token refresh handled on 401 responses
- Tokens cleared on logout

### Authorization

- Route guards prevent unauthorized access
- Backend validates all requests (frontend is not trusted)
- UI elements hidden/shown based on user role

## Performance Optimizations

### 1. Lazy Loading

All feature modules are lazy-loaded, reducing initial bundle size.

### 2. OnPush Change Detection

Use `ChangeDetectionStrategy.OnPush` for pure components:

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
})
```

### 3. TrackBy Functions

Optimize `ngFor` with `trackBy`:

```typescript
trackByProductId(index: number, product: Product): string {
  return product.id;
}
```

### 4. Image Optimization

Use lazy loading for images:

```html
<img [src]="product.imageUrl" loading="lazy" />
```

### 5. HTTP Caching

Cache API responses when appropriate (products, categories).

## Error Handling

### Global Error Handler

```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Handle unauthorized
        inject(Router).navigate(["/login"]);
      } else if (error.status === 403) {
        // Handle forbidden
      } else if (error.status >= 500) {
        // Handle server errors
      }

      return throwError(() => error);
    }),
  );
};
```

### Component-Level Error Handling

```typescript
loadProducts() {
  this.loading.set(true);
  this.error.set(null);

  this.productService.getProducts().subscribe({
    next: (products) => {
      this.products.set(products);
      this.loading.set(false);
    },
    error: (err) => {
      this.error.set('Failed to load products');
      this.loading.set(false);
      console.error(err);
    }
  });
}
```

## Testing Strategy

### Unit Tests

- Test services in isolation
- Mock HTTP calls with `HttpClientTestingModule`
- Test component logic without rendering

### Integration Tests

- Test component + template interaction
- Test user interactions (click, input)
- Verify UI updates based on state changes

### E2E Tests (Future)

- Test complete user workflows
- Use Playwright or Cypress

## Build & Deployment

### Development Build

```bash
ng serve
```

- Source maps enabled
- Hot module replacement
- No minification

### Production Build

```bash
ng build --configuration production
```

- Minification enabled
- Dead code elimination
- Optimized bundles
- Output: `dist/frontend/`

### Docker Deployment

Frontend can be served via Nginx in Docker:

```dockerfile
FROM nginx:alpine
COPY dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
```

## Future Enhancements

1. **NgRx or Akita**: If state complexity increases
2. **Service Workers**: For PWA capabilities
3. **SSR (Server-Side Rendering)**: For SEO and performance
4. **Micro-frontends**: If modules need independent deployment
5. **GraphQL**: Alternative to REST API

## Related Documentation

- [Quick Start Guide](./QUICK_START.md)
- [Developer Guide](./DEVELOPER_GUIDE.md)
- [API Integration](./API_INTEGRATION.md)
- [Component Guide](./COMPONENT_GUIDE.md)

---

**Last Updated**: February 2026 😊
