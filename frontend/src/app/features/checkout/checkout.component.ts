import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { CreateOrderRequest, OrderItemRequest } from '../../core/models/order.model';

/**
 * Checkout component for placing orders
 */
@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss'],
})
export class CheckoutComponent implements OnInit {
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);
  private isPlacingOrder = false;

  ngOnInit(): void {
    // Initial check for empty cart
    if (this.cartService.cartItems().length === 0) {
      this.router.navigate(['/cart']);
    }
  }

  /**
   * Gets cart items for display
   */
  get cartItems() {
    return this.cartService.cartItems();
  }

  /**
   * Gets cart total
   */
  get cartTotal() {
    return this.cartService.cartTotal();
  }

  /**
   * Calculates the subtotal for a cart item
   */
  getItemSubtotal(price: number, quantity: number): number {
    return price * quantity;
  }

  /**
   * Places the order
   */
  placeOrder(): void {
    if (this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.isPlacingOrder = true;

    // Build order request from cart items
    const items: OrderItemRequest[] = this.cartService.cartItems().map((item) => ({
      productId: item.product.id,
      quantity: item.quantity,
    }));

    const request: CreateOrderRequest = { items };

    this.orderService.createOrder(request).subscribe({
      next: (order) => {
        // Success: navigate to order detail first, then clear cart
        this.router.navigate(['/orders', order.id]).then(() => {
          this.cartService.clearCart();
          this.isPlacingOrder = false;
        });
      },
      error: (err) => {
        // Extract error message from RFC 7807 response
        this.loading.set(false);
        this.isPlacingOrder = false;
        const errorDetail = err.error?.detail || err.error?.message || 'Failed to place order. Please try again.';
        this.error.set(errorDetail);
      },
    });
  }
}
