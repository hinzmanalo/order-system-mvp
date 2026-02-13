import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ProductListComponent } from './product-list.component';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Product } from '../../../core/models/product.model';

describe('ProductListComponent', () => {
  let component: ProductListComponent;
  let fixture: ComponentFixture<ProductListComponent>;
  let productService: jasmine.SpyObj<ProductService>;
  let cartService: jasmine.SpyObj<CartService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  const mockProducts: Product[] = [
    {
      id: '1',
      sku: 'PROD-001',
      name: 'Product 1',
      description: 'Description 1',
      price: 10.0,
      active: true,
      createdAt: '2024-01-01T00:00:00Z'
    },
    {
      id: '2',
      sku: 'PROD-002',
      name: 'Product 2',
      description: 'Description 2',
      price: 20.0,
      active: true,
      createdAt: '2024-01-01T00:00:00Z'
    }
  ];

  const mockPageResponse = {
    content: mockProducts,
    totalPages: 1,
    totalElements: 2,
    number: 0,
    size: 12,
    first: true,
    last: true
  };

  beforeEach(async () => {
    const productServiceSpy = jasmine.createSpyObj('ProductService', ['getProducts']);
    const cartServiceSpy = jasmine.createSpyObj('CartService', ['addToCart', 'cartItems']);
    const toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [ProductListComponent, ReactiveFormsModule],
      providers: [
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;
    cartService = TestBed.inject(CartService) as jasmine.SpyObj<CartService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    productService.getProducts.and.returnValue(of(mockPageResponse));

    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load products on init', () => {
    fixture.detectChanges(); // triggers ngOnInit

    expect(productService.getProducts).toHaveBeenCalled();
    expect(component.products().length).toBe(2);
    expect(component.totalPages()).toBe(1);
    expect(component.totalElements()).toBe(2);
  });

  it('should render products correctly', () => {
    fixture.detectChanges();

    expect(component.products()[0].name).toBe('Product 1');
    expect(component.products()[1].name).toBe('Product 2');
  });

  it('should reset to page 0 when search filter changes', (done) => {
    fixture.detectChanges();

    component.currentPage.set(2);
    component.searchControl.setValue('test');

    // Wait for debounce
    setTimeout(() => {
      expect(component.currentPage()).toBe(0);
      expect(productService.getProducts).toHaveBeenCalled();
      done();
    }, 350);
  });

  it('should reset to page 0 when sort changes', () => {
    fixture.detectChanges();

    component.currentPage.set(2);
    component.sortControl.setValue('price,desc');

    expect(component.currentPage()).toBe(0);
    expect(component.sortBy()).toBe('price,desc');
  });

  it('should call loadProducts when filter changes', () => {
    fixture.detectChanges();
    const initialCalls = productService.getProducts.calls.count();

    component.loadProducts();

    expect(productService.getProducts.calls.count()).toBe(initialCalls + 1);
  });

  it('should handle error when loading products fails', () => {
    productService.getProducts.and.returnValue(
      throwError(() => new Error('Failed to load'))
    );

    fixture.detectChanges();

    expect(toastService.error).toHaveBeenCalledWith('Failed to load products. Please try again.');
    expect(component.loading()).toBe(false);
  });

  it('should pass correct parameters to getProducts', () => {
    component.searchName.set('laptop');
    component.minPrice.set(100);
    component.maxPrice.set(500);
    component.sortBy.set('price,asc');
    component.currentPage.set(1);

    component.loadProducts();

    expect(productService.getProducts).toHaveBeenCalledWith({
      page: 1,
      size: 12,
      name: 'laptop',
      minPrice: 100,
      maxPrice: 500,
      sort: 'price,asc'
    });
  });

  it('should omit undefined filter values', () => {
    component.searchName.set('');
    component.minPrice.set(null);
    component.maxPrice.set(null);

    component.loadProducts();

    const callArgs = productService.getProducts.calls.mostRecent().args[0];
    expect(callArgs.name).toBeUndefined();
    expect(callArgs.minPrice).toBeUndefined();
    expect(callArgs.maxPrice).toBeUndefined();
  });
});
