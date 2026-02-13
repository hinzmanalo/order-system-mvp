import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../core/services/cart.service';

/**
 * Shopping cart component for viewing and managing cart items
 */
@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss'],
})
export class CartComponent {
  cartService = inject(CartService);

  /**
   * Updates the quantity of a cart item
   */
  updateQuantity(productId: string, quantity: number): void {
    if (quantity > 0) {
      this.cartService.updateQuantity(productId, quantity);
    }
  }

  /**
   * Removes an item from the cart
   */
  removeItem(productId: string): void {
    this.cartService.removeFromCart(productId);
  }

  /**
   * Calculates the subtotal for a cart item
   */
  getItemSubtotal(price: number, quantity: number): number {
    return price * quantity;
  }
}
