import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';
import { Product } from '../models/product.model';
import { CartItem } from '../models/cart.model';

describe('CartService', () => {
  let service: CartService;

  const mockProduct1: Product = {
    id: '1',
    sku: 'PROD-001',
    name: 'Product 1',
    description: 'Description 1',
    price: 10.0,
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  };

  const mockProduct2: Product = {
    id: '2',
    sku: 'PROD-002',
    name: 'Product 2',
    description: 'Description 2',
    price: 25.0,
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CartService]
    });
    service = TestBed.inject(CartService);
    
    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('addToCart', () => {
    it('should add new product to cart', () => {
      service.addToCart(mockProduct1, 2);

      const items = service.cartItems();
      expect(items.length).toBe(1);
      expect(items[0].product.id).toBe('1');
      expect(items[0].quantity).toBe(2);
    });

    it('should increment quantity of existing product', () => {
      service.addToCart(mockProduct1, 2);
      service.addToCart(mockProduct1, 3);

      const items = service.cartItems();
      expect(items.length).toBe(1);
      expect(items[0].quantity).toBe(5);
    });

    it('should add multiple different products', () => {
      service.addToCart(mockProduct1, 2);
      service.addToCart(mockProduct2, 1);

      const items = service.cartItems();
      expect(items.length).toBe(2);
      expect(items[0].product.id).toBe('1');
      expect(items[1].product.id).toBe('2');
    });

    it('should persist cart to localStorage', () => {
      service.addToCart(mockProduct1, 2);

      const stored = localStorage.getItem('cart');
      expect(stored).toBeTruthy();
      
      const parsed = JSON.parse(stored!);
      expect(parsed.length).toBe(1);
      expect(parsed[0].quantity).toBe(2);
    });
  });

  describe('removeFromCart', () => {
    it('should remove product from cart', () => {
      service.addToCart(mockProduct1, 2);
      service.addToCart(mockProduct2, 1);
      
      service.removeFromCart('1');

      const items = service.cartItems();
      expect(items.length).toBe(1);
      expect(items[0].product.id).toBe('2');
    });

    it('should update localStorage after removal', () => {
      service.addToCart(mockProduct1, 2);
      service.removeFromCart('1');

      const stored = localStorage.getItem('cart');
      const parsed = JSON.parse(stored!);
      expect(parsed.length).toBe(0);
    });
  });

  describe('updateQuantity', () => {
    it('should update quantity of existing product', () => {
      service.addToCart(mockProduct1, 2);
      service.updateQuantity('1', 5);

      const items = service.cartItems();
      expect(items[0].quantity).toBe(5);
    });

    it('should remove product when quantity is zero', () => {
      service.addToCart(mockProduct1, 2);
      service.updateQuantity('1', 0);

      const items = service.cartItems();
      expect(items.length).toBe(0);
    });

    it('should remove product when quantity is negative', () => {
      service.addToCart(mockProduct1, 2);
      service.updateQuantity('1', -1);

      const items = service.cartItems();
      expect(items.length).toBe(0);
    });

    it('should persist updated quantity to localStorage', () => {
      service.addToCart(mockProduct1, 2);
      service.updateQuantity('1', 10);

      const stored = localStorage.getItem('cart');
      const parsed = JSON.parse(stored!);
      expect(parsed[0].quantity).toBe(10);
    });
  });

  describe('clearCart', () => {
    it('should remove all items from cart', () => {
      service.addToCart(mockProduct1, 2);
      service.addToCart(mockProduct2, 1);
      
      service.clearCart();

      const items = service.cartItems();
      expect(items.length).toBe(0);
    });

    it('should clear localStorage', () => {
      service.addToCart(mockProduct1, 2);
      service.clearCart();

      const stored = localStorage.getItem('cart');
      const parsed = JSON.parse(stored!);
      expect(parsed.length).toBe(0);
    });
  });

  describe('computed signals', () => {
    it('should calculate cart count correctly', () => {
      expect(service.cartCount()).toBe(0);

      service.addToCart(mockProduct1, 2);
      expect(service.cartCount()).toBe(2);

      service.addToCart(mockProduct2, 3);
      expect(service.cartCount()).toBe(5);
    });

    it('should calculate cart total correctly', () => {
      expect(service.cartTotal()).toBe(0);

      service.addToCart(mockProduct1, 2); // 2 * 10 = 20
      expect(service.cartTotal()).toBe(20);

      service.addToCart(mockProduct2, 1); // 1 * 25 = 25
      expect(service.cartTotal()).toBe(45);
    });

    it('should update computed values when cart changes', () => {
      service.addToCart(mockProduct1, 2);
      expect(service.cartCount()).toBe(2);
      expect(service.cartTotal()).toBe(20);

      service.updateQuantity('1', 5);
      expect(service.cartCount()).toBe(5);
      expect(service.cartTotal()).toBe(50);

      service.removeFromCart('1');
      expect(service.cartCount()).toBe(0);
      expect(service.cartTotal()).toBe(0);
    });
  });

  describe('localStorage persistence', () => {
    it('should load cart from localStorage on initialization', () => {
      // Manually set localStorage
      const mockCart: CartItem[] = [
        { product: mockProduct1, quantity: 3 }
      ];
      localStorage.setItem('cart', JSON.stringify(mockCart));

      // Create new service instance
      const newService = new CartService();

      expect(newService.cartItems().length).toBe(1);
      expect(newService.cartItems()[0].quantity).toBe(3);
      expect(newService.cartCount()).toBe(3);
    });

    it('should handle invalid localStorage data gracefully', () => {
      localStorage.setItem('cart', 'invalid-json');

      const newService = new CartService();

      expect(newService.cartItems().length).toBe(0);
    });

    it('should handle missing localStorage data', () => {
      const newService = new CartService();

      expect(newService.cartItems().length).toBe(0);
    });
  });
});
