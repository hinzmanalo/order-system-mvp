# OrderHub Frontend - Component Guide

## Overview

This guide provides detailed information on component architecture, patterns, and best practices for building Angular components in the OrderHub application.

## Table of Contents

1. [Component Types](#component-types)
2. [Standalone Components](#standalone-components)
3. [Smart vs. Presentational Components](#smart-vs-presentational-components)
4. [Component Communication](#component-communication)
5. [State Management](#state-management)
6. [Lifecycle Hooks](#lifecycle-hooks)
7. [Forms](#forms)
8. [Styling](#styling)
9. [Common Patterns](#common-patterns)
10. [Component Catalog](#component-catalog)

---

## Component Types

### Feature Components

Location: `src/app/features/{feature-name}/`

Feature components implement complete user-facing features and business logic.

**Examples**:

- `ProductListComponent` - Display paginated product catalog
- `CheckoutComponent` - Handle checkout process
- `OrderDetailComponent` - Show order details

### Shared Components

Location: `src/app/shared/components/{component-name}/`

Reusable UI components used across multiple features.

**Examples**:

- `NavbarComponent` - Application navigation
- `LoadingSpinnerComponent` - Loading indicator
- `ErrorMessageComponent` - Error display
- `ModalComponent` - Modal dialog

### Layout Components

Components that define page structure and layout.

**Examples**:

- `AppComponent` - Root component
- `MainLayoutComponent` - Main application layout
- `AdminLayoutComponent` - Admin panel layout

---

## Standalone Components

All OrderHub components are **standalone components** (no NgModules).

### Basic Structure

```typescript
import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-my-component",
  standalone: true,
  imports: [CommonModule], // Explicitly import dependencies
  templateUrl: "./my-component.component.html",
  styleUrl: "./my-component.component.scss",
})
export class MyComponent {
  // Component logic
}
```

### Benefits

- ✅ **Explicit dependencies**: Clear what each component uses
- ✅ **Better tree-shaking**: Unused code eliminated
- ✅ **Simpler mental model**: No module configuration
- ✅ **Easier to refactor**: Self-contained components

---

## Smart vs. Presentational Components

### Smart (Container) Components

**Responsibilities**:

- Manage state and business logic
- Interact with services
- Handle routing and navigation
- Fetch and persist data
- Coordinate child components

**Pattern**:

```typescript
@Component({
  selector: "app-product-list",
  standalone: true,
  imports: [CommonModule, ProductCardComponent],
  template: `
    @if (loading()) {
      <app-loading-spinner />
    }

    @if (error()) {
      <app-error-message [message]="error()" />
    }

    <div class="products-grid">
      @for (product of products(); track product.id) {
        <app-product-card [product]="product" (addToCart)="handleAddToCart($event)" />
      }
    </div>
  `,
})
export class ProductListComponent {
  // State management
  products = signal<Product[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private router: Router,
  ) {
    this.loadProducts();
  }

  private loadProducts() {
    this.loading.set(true);
    this.productService.getProducts().subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set("Failed to load products");
        this.loading.set(false);
      },
    });
  }

  handleAddToCart(product: Product) {
    this.cartService.addItem({ ...product, quantity: 1 });
    this.router.navigate(["/cart"]);
  }
}
```

### Presentational (Dumb) Components

**Responsibilities**:

- Pure UI rendering
- Receive data via `@Input()`
- Emit events via `@Output()`
- No service injection (except utility services)
- Highly reusable

**Pattern**:

```typescript
@Component({
  selector: "app-product-card",
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="product-card">
      <img [src]="product().imageUrl" [alt]="product().name" />
      <h3>{{ product().name }}</h3>
      <p class="price">{{ product().price | currency }}</p>
      <p class="description">{{ product().description }}</p>
      <button class="add-to-cart-btn" (click)="onAddToCartClick()" [disabled]="!isAvailable()">Add to Cart</button>
    </div>
  `,
})
export class ProductCardComponent {
  // Input using signals
  product = input.required<Product>();

  // Output events
  addToCart = output<Product>();

  // Computed values
  isAvailable = computed(() => {
    return this.product().inventory?.quantity > 0;
  });

  onAddToCartClick() {
    this.addToCart.emit(this.product());
  }
}
```

**Key Differences**:

| Aspect      | Smart Components      | Presentational Components      |
| ----------- | --------------------- | ------------------------------ |
| State       | Manages complex state | No internal state (or minimal) |
| Services    | Injects services      | No service injection           |
| Reusability | Feature-specific      | Highly reusable                |
| Testing     | Requires mocking      | Easy to test                   |
| Location    | `features/`           | `shared/`                      |

---

## Component Communication

### Parent to Child: @Input()

**Using Signals (Angular 17+)**:

```typescript
// Child component
export class ChildComponent {
  // Required input
  data = input.required<string>();

  // Optional input with default
  count = input<number>(0);

  // Computed based on input
  displayText = computed(() => `Count: ${this.count()}`);
}

// Parent component
@Component({
  template: ` <app-child [data]="myData" [count]="5" /> `,
})
export class ParentComponent {
  myData = "Hello World";
}
```

**Traditional Approach**:

```typescript
// Child component
export class ChildComponent {
  @Input({ required: true }) data!: string;
  @Input() count: number = 0;
}
```

### Child to Parent: @Output()

```typescript
// Child component
export class ChildComponent {
  itemClicked = output<Item>();

  handleClick(item: Item) {
    this.itemClicked.emit(item);
  }
}

// Parent component
@Component({
  template: ` <app-child (itemClicked)="onItemClicked($event)" /> `,
})
export class ParentComponent {
  onItemClicked(item: Item) {
    console.log("Item clicked:", item);
  }
}
```

### Sibling Communication: Shared Service

```typescript
// Shared service
@Injectable({ providedIn: "root" })
export class CartService {
  private items = signal<CartItem[]>([]);
  readonly items$ = computed(() => this.items());

  addItem(item: CartItem) {
    this.items.update((items) => [...items, item]);
  }
}

// Component A
export class ProductListComponent {
  constructor(private cartService: CartService) {}

  addToCart(product: Product) {
    this.cartService.addItem({ ...product, quantity: 1 });
  }
}

// Component B
export class CartBadgeComponent {
  constructor(private cartService: CartService) {}

  itemCount = computed(() => this.cartService.items$().length);
}
```

### Content Projection: ng-content

```typescript
// Container component
@Component({
  selector: 'app-card',
  template: `
    <div class="card">
      <div class="card-header">
        <ng-content select="[slot='header']" />
      </div>
      <div class="card-body">
        <ng-content />
      </div>
      <div class="card-footer">
        <ng-content select="[slot='footer']" />
      </div>
    </div>
  `,
})
export class CardComponent {}

// Usage
<app-card>
  <h2 slot="header">Title</h2>
  <p>Body content goes here</p>
  <button slot="footer">Action</button>
</app-card>
```

---

## State Management

### Local Component State with Signals

```typescript
export class ProductListComponent {
  // Writable signals
  products = signal<Product[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  // Computed signals (auto-update)
  productCount = computed(() => this.products().length);
  hasProducts = computed(() => this.products().length > 0);

  // Update signal values
  loadProducts(newProducts: Product[]) {
    this.products.set(newProducts); // Replace
  }

  addProduct(product: Product) {
    this.products.update((products) => [...products, product]); // Append
  }

  removeProduct(id: string) {
    this.products.update((products) => products.filter((p) => p.id !== id));
  }
}
```

### Effects (React to Signal Changes)

```typescript
export class CartComponent {
  cartItems = signal<CartItem[]>([]);

  constructor() {
    // Effect runs whenever cartItems changes
    effect(() => {
      const items = this.cartItems();
      console.log("Cart updated:", items.length, "items");

      // Persist to localStorage
      localStorage.setItem("cart", JSON.stringify(items));
    });
  }
}
```

### Service-Based State

```typescript
@Injectable({ providedIn: "root" })
export class AuthService {
  // Private state
  private currentUser = signal<User | null>(null);

  // Public readonly access
  readonly user$ = computed(() => this.currentUser());
  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly isAdmin = computed(() => this.currentUser()?.role === "ADMIN");

  // Mutations
  login(user: User) {
    this.currentUser.set(user);
  }

  logout() {
    this.currentUser.set(null);
  }
}
```

---

## Lifecycle Hooks

### Common Hooks

```typescript
export class MyComponent implements OnInit, OnDestroy, OnChanges {
  // Called after constructor, before view initialized
  ngOnInit() {
    console.log("Component initialized");
    this.loadData();
  }

  // Called when input properties change
  ngOnChanges(changes: SimpleChanges) {
    if (changes["productId"]) {
      this.loadProduct(this.productId);
    }
  }

  // Called when component is destroyed
  ngOnDestroy() {
    console.log("Component destroyed");
    this.cleanup();
  }

  // Called after view is initialized
  ngAfterViewInit() {
    console.log("View initialized");
  }
}
```

### Modern Approach with Signals

```typescript
import { Component, effect, input } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

export class MyComponent {
  productId = input<string>();

  constructor() {
    // Effect runs when productId changes
    effect(() => {
      const id = this.productId();
      if (id) {
        this.loadProduct(id);
      }
    });

    // Auto-cleanup on component destroy
    this.productService
      .getProducts()
      .pipe(takeUntilDestroyed())
      .subscribe((products) => this.products.set(products));
  }
}
```

---

## Forms

### Reactive Forms (Recommended)

```typescript
import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
      <div class="form-group">
        <label for="email">Email</label>
        <input id="email" type="email" formControlName="email" [class.error]="emailErrors()" />
        @if (emailErrors()) {
          <span class="error-message">{{ emailErrors() }}</span>
        }
      </div>

      <div class="form-group">
        <label for="password">Password</label>
        <input id="password" type="password" formControlName="password" [class.error]="passwordErrors()" />
        @if (passwordErrors()) {
          <span class="error-message">{{ passwordErrors() }}</span>
        }
      </div>

      <button type="submit" [disabled]="loginForm.invalid || isSubmitting()">
        {{ isSubmitting() ? "Logging in..." : "Login" }}
      </button>
    </form>
  `,
})
export class LoginComponent {
  isSubmitting = signal(false);

  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {
    this.loginForm = this.fb.group({
      email: ["", [Validators.required, Validators.email]],
      password: ["", [Validators.required, Validators.minLength(8)]],
    });
  }

  emailErrors(): string | null {
    const control = this.loginForm.get("email");
    if (control?.hasError("required") && control.touched) {
      return "Email is required";
    }
    if (control?.hasError("email") && control.touched) {
      return "Invalid email format";
    }
    return null;
  }

  passwordErrors(): string | null {
    const control = this.loginForm.get("password");
    if (control?.hasError("required") && control.touched) {
      return "Password is required";
    }
    if (control?.hasError("minlength") && control.touched) {
      return "Password must be at least 8 characters";
    }
    return null;
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.isSubmitting.set(true);

      const { email, password } = this.loginForm.value;

      this.authService.login(email, password).subscribe({
        next: () => {
          this.router.navigate(["/products"]);
        },
        error: (err) => {
          alert("Login failed: " + err.message);
          this.isSubmitting.set(false);
        },
      });
    }
  }
}
```

### Template-Driven Forms (Simple Cases)

```typescript
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";

@Component({
  selector: "app-search",
  standalone: true,
  imports: [FormsModule],
  template: ` <input [(ngModel)]="searchQuery" (ngModelChange)="onSearchChange()" placeholder="Search products..." /> `,
})
export class SearchComponent {
  searchQuery = "";

  onSearchChange() {
    console.log("Search:", this.searchQuery);
  }
}
```

---

## Styling

### Component Styles (Scoped)

```scss
// product-card.component.scss
.product-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1rem;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }

  &__image {
    width: 100%;
    aspect-ratio: 1;
    object-fit: cover;
    border-radius: 4px;
  }

  &__title {
    font-size: 1.25rem;
    font-weight: 600;
    margin: 0.5rem 0;
  }

  &__price {
    font-size: 1.5rem;
    color: var(--primary-color);
    font-weight: bold;
  }

  &--featured {
    border: 2px solid var(--primary-color);
  }
}
```

### Host Styling

```typescript
@Component({
  selector: "app-product-card",
  styles: [
    `
      :host {
        display: block;
        width: 100%;
        max-width: 300px;
      }

      :host.compact {
        max-width: 200px;
      }
    `,
  ],
})
export class ProductCardComponent {}
```

### Dynamic Classes

```html
<div class="product-card" [class.featured]="product().featured" [class.out-of-stock]="product().inventory.quantity === 0"></div>
```

Or with `ngClass`:

```html
<div
  [ngClass]="{
  'product-card': true,
  'featured': product().featured,
  'out-of-stock': product().inventory.quantity === 0
}"
></div>
```

---

## Common Patterns

### Loading State Pattern

```typescript
export class DataComponent {
  data = signal<Data | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  loadData() {
    this.loading.set(true);
    this.error.set(null);

    this.service.getData().subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message);
        this.loading.set(false);
      },
    });
  }
}
```

Template:

```html
@if (loading()) {
<app-loading-spinner />
} @if (error()) {
<app-error-message [message]="error()" />
} @if (data() && !loading()) {
<div>{{ data() }}</div>
}
```

### Pagination Pattern

```typescript
export class PaginatedListComponent {
  items = signal<Item[]>([]);
  currentPage = signal(0);
  pageSize = signal(20);
  totalPages = signal(0);

  loadPage(page: number) {
    this.currentPage.set(page);

    this.service.getItems({ page, size: this.pageSize() }).subscribe({
      next: (response) => {
        this.items.set(response.content);
        this.totalPages.set(response.totalPages);
      },
    });
  }

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) {
      this.loadPage(this.currentPage() + 1);
    }
  }

  previousPage() {
    if (this.currentPage() > 0) {
      this.loadPage(this.currentPage() - 1);
    }
  }
}
```

### Confirmation Dialog Pattern

```typescript
export class DeleteProductComponent {
  showConfirmation = signal(false);

  handleDelete() {
    this.showConfirmation.set(true);
  }

  confirmDelete() {
    this.productService.delete(this.productId()).subscribe({
      next: () => {
        this.showConfirmation.set(false);
        this.router.navigate(["/products"]);
      },
    });
  }

  cancelDelete() {
    this.showConfirmation.set(false);
  }
}
```

---

## Component Catalog

### Core Components

#### NavbarComponent

**Location**: `shared/components/navbar/`

**Purpose**: Application navigation bar

**Inputs**:

- `user`: Current user (optional)
- `cartItemCount`: Number of items in cart

**Outputs**:

- `logout`: Emits when user logs out

#### LoadingSpinnerComponent

**Location**: `shared/components/loading-spinner/`

**Purpose**: Display loading indicator

**Inputs**:

- `size`: 'small' | 'medium' | 'large'
- `message`: Optional loading message

#### ErrorMessageComponent

**Location**: `shared/components/error-message/`

**Purpose**: Display error messages

**Inputs**:

- `message`: Error message text
- `type`: 'error' | 'warning' | 'info'

**Outputs**:

- `retry`: Emits when user clicks retry

### Feature Components

#### ProductListComponent

**Location**: `features/catalog/product-list/`

**Purpose**: Display paginated product catalog

**Features**:

- Pagination
- Filtering
- Sorting
- Add to cart

#### ProductDetailComponent

**Location**: `features/catalog/product-detail/`

**Purpose**: Display product details

**Features**:

- Product information
- Inventory status
- Add to cart
- Quantity selection

#### CartComponent

**Location**: `features/cart/`

**Purpose**: Shopping cart management

**Features**:

- List cart items
- Update quantities
- Remove items
- Calculate totals
- Proceed to checkout

#### CheckoutComponent

**Location**: `features/checkout/`

**Purpose**: Order checkout process

**Features**:

- Order summary
- Payment form
- Order confirmation

---

**For more information, see [Architecture Guide](./ARCHITECTURE.md) and [Developer Guide](./DEVELOPER_GUIDE.md) 😊**
