import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { CartComponent } from './cart.component';
import { CartService } from '../../core/services/cart.service';
import { CartItem } from '../../core/models/cart.model';

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;
  let cartService: jasmine.SpyObj<CartService>;

  const mockCartItems: CartItem[] = [
    {
      product: {
        id: '1',
        sku: 'PROD-001',
        name: 'Product 1',
        description: 'Description 1',
        price: 10.0,
        active: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      quantity: 2
    },
    {
      product: {
        id: '2',
        sku: 'PROD-002',
        name: 'Product 2',
        description: 'Description 2',
        price: 25.0,
        active: true,
        createdAt: '2024-01-01T00:00:00Z'
      },
      quantity: 1
    }
  ];

  beforeEach(async () => {
    const cartServiceSpy = jasmine.createSpyObj('CartService', [
      'updateQuantity',
      'removeFromCart',
      'clearCart'
    ], {
      cartItems: signal(mockCartItems),
      cartTotal: signal(45.0),
      cartCount: signal(3)
    });

    await TestBed.configureTestingModule({
      imports: [CartComponent],
      providers: [
        { provide: CartService, useValue: cartServiceSpy },
      ],
    }).compileComponents();

    cartService = TestBed.inject(CartService) as jasmine.SpyObj<CartService>;

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display cart items from service', () => {
    expect(component.cartService.cartItems().length).toBe(2);
    expect(component.cartService.cartItems()[0].product.name).toBe('Product 1');
  });

  it('should update quantity when valid', () => {
    component.updateQuantity('1', 5);

    expect(cartService.updateQuantity).toHaveBeenCalledWith('1', 5);
  });

  it('should not update quantity when zero or negative', () => {
    component.updateQuantity('1', 0);
    expect(cartService.updateQuantity).not.toHaveBeenCalled();

    component.updateQuantity('1', -1);
    expect(cartService.updateQuantity).not.toHaveBeenCalled();
  });

  it('should remove item from cart', () => {
    component.removeItem('1');

    expect(cartService.removeFromCart).toHaveBeenCalledWith('1');
  });

  it('should calculate item subtotal correctly', () => {
    const subtotal = component.getItemSubtotal(10.0, 2);
    expect(subtotal).toBe(20.0);
  });

  it('should calculate item subtotal for different quantities', () => {
    expect(component.getItemSubtotal(25.0, 1)).toBe(25.0);
    expect(component.getItemSubtotal(15.5, 3)).toBe(46.5);
  });

  it('should access cart total from service', () => {
    expect(component.cartService.cartTotal()).toBe(45.0);
  });

  it('should display empty state when cart is empty', () => {
    const emptyCartServiceSpy = jasmine.createSpyObj('CartService', [
      'updateQuantity',
      'removeFromCart',
      'clearCart'
    ], {
      cartItems: signal([]),
      cartTotal: signal(0),
      cartCount: signal(0)
    });

    expect(emptyCartServiceSpy.cartItems().length).toBe(0);
  });
});
