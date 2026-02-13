import { Injectable, signal, computed } from '@angular/core';
import { CartItem } from '../models/cart.model';
import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly CART_STORAGE_KEY = 'cart';

  // Signals for reactive state
  cartItems = signal<CartItem[]>(this.loadCartFromStorage());
  cartCount = computed(() =>
    this.cartItems().reduce((sum, item) => sum + item.quantity, 0)
  );
  cartTotal = computed(() =>
    this.cartItems().reduce(
      (sum, item) => sum + item.product.price * item.quantity,
      0
    )
  );

  /**
   * Adds a product to the cart or increments quantity if already present
   */
  addToCart(product: Product, quantity: number): void {
    const currentItems = this.cartItems();
    const existingItem = currentItems.find((item) => item.product.id === product.id);

    let updatedItems: CartItem[];
    if (existingItem) {
      // Increment quantity of existing item
      updatedItems = currentItems.map((item) =>
        item.product.id === product.id
          ? { ...item, quantity: item.quantity + quantity }
          : item
      );
    } else {
      // Add new item to cart
      updatedItems = [...currentItems, { product, quantity }];
    }

    this.cartItems.set(updatedItems);
    this.persistCart(updatedItems);
  }

  /**
   * Removes an item from the cart
   */
  removeFromCart(productId: string): void {
    const updatedItems = this.cartItems().filter(
      (item) => item.product.id !== productId
    );
    this.cartItems.set(updatedItems);
    this.persistCart(updatedItems);
  }

  /**
   * Updates the quantity of a cart item
   */
  updateQuantity(productId: string, quantity: number): void {
    if (quantity <= 0) {
      this.removeFromCart(productId);
      return;
    }

    const updatedItems = this.cartItems().map((item) =>
      item.product.id === productId ? { ...item, quantity } : item
    );
    this.cartItems.set(updatedItems);
    this.persistCart(updatedItems);
  }

  /**
   * Clears all items from the cart
   */
  clearCart(): void {
    this.cartItems.set([]);
    this.persistCart([]);
  }

  /**
   * Persists cart items to localStorage
   */
  private persistCart(items: CartItem[]): void {
    try {
      localStorage.setItem(this.CART_STORAGE_KEY, JSON.stringify(items));
    } catch (error) {
      console.error('Failed to persist cart to localStorage', error);
    }
  }

  /**
   * Loads cart items from localStorage
   */
  private loadCartFromStorage(): CartItem[] {
    try {
      const stored = localStorage.getItem(this.CART_STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch (error) {
      console.error('Failed to load cart from localStorage', error);
      return [];
    }
  }
}
