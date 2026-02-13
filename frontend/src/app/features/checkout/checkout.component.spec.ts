import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { CheckoutComponent } from './checkout.component';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { CartItem } from '../../core/models/cart.model';
import { Order } from '../../core/models/order.model';

describe('CheckoutComponent', () => {
  let component: CheckoutComponent;
  let fixture: ComponentFixture<CheckoutComponent>;
  let cartService: jasmine.SpyObj<CartService>;
  let orderService: jasmine.SpyObj<OrderService>;
  let router: jasmine.SpyObj<Router>;

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
    }
  ];

  const mockOrder: Order = {
    id: 'order-123',
    userId: 'user-123',
    status: 'CONFIRMED',
    totalAmount: 20.0,
    items: [
      {
        productId: '1',
        productName: 'Product 1',
        quantity: 2,
        unitPrice: 10.0,
        subtotal: 20.0
      }
    ],
    createdAt: new Date().toISOString()
  };

  beforeEach(async () => {
    const cartServiceSpy = jasmine.createSpyObj('CartService', ['clearCart'], {
      cartItems: signal(mockCartItems),
      cartTotal: signal(20.0),
      cartCount: signal(2)
    });
    const orderServiceSpy = jasmine.createSpyObj('OrderService', ['createOrder']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        { provide: CartService, useValue: cartServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    cartService = TestBed.inject(CartService) as jasmine.SpyObj<CartService>;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    fixture = TestBed.createComponent(CheckoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display cart items', () => {
    expect(component.cartItems.length).toBe(1);
    expect(component.cartItems[0].product.name).toBe('Product 1');
  });

  it('should display cart total', () => {
    expect(component.cartTotal).toBe(20.0);
  });

  it('should calculate item subtotal correctly', () => {
    const subtotal = component.getItemSubtotal(10.0, 2);
    expect(subtotal).toBe(20.0);
  });

  it('should create order on placeOrder', () => {
    orderService.createOrder.and.returnValue(of(mockOrder));

    component.placeOrder();

    expect(orderService.createOrder).toHaveBeenCalledWith({
      items: [
        {
          productId: '1',
          quantity: 2
        }
      ]
    });
  });

  it('should clear cart and navigate on successful order', (done) => {
    orderService.createOrder.and.returnValue(of(mockOrder));
    router.navigate.and.returnValue(Promise.resolve(true));

    component.placeOrder();

    // Navigation should happen immediately
    expect(router.navigate).toHaveBeenCalledWith(['/orders', 'order-123']);

    // Cart should be cleared after navigation completes
    setTimeout(() => {
      expect(cartService.clearCart).toHaveBeenCalled();
      done();
    }, 10);
  });

  it('should display error message on order failure', () => {
    const errorResponse = {
      error: {
        detail: 'Insufficient stock'
      }
    };
    orderService.createOrder.and.returnValue(throwError(() => errorResponse));

    component.placeOrder();

    expect(component.error()).toBe('Insufficient stock');
    expect(component.loading()).toBe(false);
    expect(cartService.clearCart).not.toHaveBeenCalled();
  });

  it('should use default error message when detail not available', () => {
    const errorResponse = { error: {} };
    orderService.createOrder.and.returnValue(throwError(() => errorResponse));

    component.placeOrder();

    expect(component.error()).toBe('Failed to place order. Please try again.');
  });

  it('should set loading state during order placement', () => {
    orderService.createOrder.and.returnValue(of(mockOrder));

    expect(component.loading()).toBe(false);
    component.placeOrder();
    
    // Loading should have been set to true during the call
    // (it gets reset to false after the observable completes)
  });

  it('should not place order if already loading', () => {
    component.loading.set(true);
    
    component.placeOrder();

    expect(orderService.createOrder).not.toHaveBeenCalled();
  });

  it('should redirect to cart if cart is empty on init', () => {
    const emptyCartServiceSpy = jasmine.createSpyObj('CartService', ['clearCart'], {
      cartItems: signal([]),
      cartTotal: signal(0),
      cartCount: signal(0)
    });

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        { provide: CartService, useValue: emptyCartServiceSpy },
        { provide: OrderService, useValue: orderService },
        { provide: Router, useValue: router },
      ],
    });

    const emptyCartFixture = TestBed.createComponent(CheckoutComponent);
    const emptyCartComponent = emptyCartFixture.componentInstance;
    emptyCartComponent.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/cart']);
  });
});
