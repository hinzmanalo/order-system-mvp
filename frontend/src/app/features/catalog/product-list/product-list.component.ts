import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { Product } from '../../../core/models/product.model';

/**
 * Product list component with filtering, sorting, and pagination
 */
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss'],
})
export class ProductListComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  // State signals
  products = signal<Product[]>([]);
  loading = signal(true);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  
  // Filter controls
  searchControl = new FormControl('');
  minPriceControl = new FormControl<number | null>(null);
  maxPriceControl = new FormControl<number | null>(null);
  sortControl = new FormControl('name,asc');

  // Filter values
  searchName = signal('');
  minPrice = signal<number | null>(null);
  maxPrice = signal<number | null>(null);
  sortBy = signal('name,asc');

  constructor() {
    // Setup search debouncing
    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((value) => {
        this.searchName.set(value || '');
        this.currentPage.set(0);
        this.loadProducts();
      });

    // Setup other filter changes
    this.sortControl.valueChanges.subscribe((value) => {
      this.sortBy.set(value || 'name,asc');
      this.currentPage.set(0);
      this.loadProducts();
    });
  }

  ngOnInit(): void {
    this.loadProducts();
  }

  /**
   * Loads products based on current filters and pagination
   */
  loadProducts(): void {
    this.loading.set(true);

    const params = {
      page: this.currentPage(),
      size: 12,
      name: this.searchName() || undefined,
      minPrice: this.minPrice() ?? undefined,
      maxPrice: this.maxPrice() ?? undefined,
      sort: this.sortBy(),
    };

    this.productService.getProducts(params).subscribe({
      next: (response) => {
        this.products.set(response.content);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.currentPage.set(response.number);
        this.loading.set(false);
      },
      error: (error) => {
        this.toastService.error('Failed to load products. Please try again.');
        this.loading.set(false);
      },
    });
  }

  /**
   * Applies price range filters
   */
  applyFilters(): void {
    this.minPrice.set(this.minPriceControl.value);
    this.maxPrice.set(this.maxPriceControl.value);
    this.currentPage.set(0);
    this.loadProducts();
  }

  /**
   * Clears all filters
   */
  clearFilters(): void {
    this.searchControl.setValue('');
    this.minPriceControl.setValue(null);
    this.maxPriceControl.setValue(null);
    this.sortControl.setValue('name,asc');
    this.searchName.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.sortBy.set('name,asc');
    this.currentPage.set(0);
    this.loadProducts();
  }

  /**
   * Handles page changes
   */
  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /**
   * Navigates to product detail page
   */
  viewProduct(product: Product): void {
    this.router.navigate(['/products', product.id]);
  }

  /**
   * Adds product to cart
   */
  addToCart(event: Event, product: Product): void {
    event.stopPropagation();
    this.cartService.addToCart(product, 1);
    this.toastService.success(`${product.name} added to cart!`);
  }
}
