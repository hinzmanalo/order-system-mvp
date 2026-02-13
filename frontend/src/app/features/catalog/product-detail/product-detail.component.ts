import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Product } from '../../../core/models/product.model';

/**
 * Product detail component displaying full product information
 */
@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
})
export class ProductDetailComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // State signals
  product = signal<Product | null>(null);
  loading = signal(true);
  notFound = signal(false);
  quantity = signal(1);

  ngOnInit(): void {
    const productId = this.route.snapshot.paramMap.get('id');
    if (productId) {
      this.loadProduct(productId);
    } else {
      this.notFound.set(true);
      this.loading.set(false);
    }
  }

  /**
   * Loads product details by ID
   */
  loadProduct(id: string): void {
    this.loading.set(true);
    this.notFound.set(false);

    this.productService.getProductById(id).subscribe({
      next: (product) => {
        this.product.set(product);
        this.loading.set(false);
      },
      error: (error) => {
        if (error.status === 404) {
          this.notFound.set(true);
        } else {
          this.toastService.error('Failed to load product. Please try again.');
        }
        this.loading.set(false);
      },
    });
  }

  /**
   * Updates the quantity value
   */
  updateQuantity(value: number): void {
    if (value >= 1) {
      this.quantity.set(value);
    }
  }

  /**
   * Increments quantity
   */
  incrementQuantity(): void {
    this.quantity.set(this.quantity() + 1);
  }

  /**
   * Decrements quantity (minimum 1)
   */
  decrementQuantity(): void {
    if (this.quantity() > 1) {
      this.quantity.set(this.quantity() - 1);
    }
  }

  /**
   * Adds product to cart with selected quantity
   */
  addToCart(): void {
    const currentProduct = this.product();
    if (!currentProduct) return;

    this.cartService.addToCart(currentProduct, this.quantity());
    this.toastService.success(`${currentProduct.name} added to cart!`);
  }

  /**
   * Adds product to cart and navigates to cart page
   */
  addToCartAndCheckout(): void {
    const currentProduct = this.product();
    if (!currentProduct) return;

    this.cartService.addToCart(currentProduct, this.quantity());
    this.toastService.success(`${currentProduct.name} added to cart!`);
    this.router.navigate(['/cart']);
  }

  /**
   * Navigates back to product list
   */
  goBack(): void {
    this.router.navigate(['/products']);
  }
}
