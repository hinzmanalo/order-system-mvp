import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { Order, OrderPage, CreateOrderRequest } from '../models/order.model';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  const mockOrder: Order = {
    id: 'order-123',
    userId: 'user-123',
    status: 'CONFIRMED',
    totalAmount: 99.99,
    items: [
      {
        productId: 'prod-1',
        productName: 'Product 1',
        quantity: 2,
        unitPrice: 49.995,
        subtotal: 99.99
      }
    ],
    createdAt: '2024-01-01T12:00:00Z'
  };

  const mockOrderPage: OrderPage = {
    content: [mockOrder],
    totalPages: 1,
    totalElements: 1,
    number: 0,
    size: 10,
    first: true,
    last: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrderService]
    });

    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createOrder', () => {
    it('should make POST request to create order', () => {
      const createRequest: CreateOrderRequest = {
        items: [
          { productId: 'prod-1', quantity: 2 }
        ]
      };

      service.createOrder(createRequest).subscribe((order) => {
        expect(order).toEqual(mockOrder);
      });

      const req = httpMock.expectOne('/api/v1/orders');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(mockOrder);
    });
  });

  describe('getMyOrders', () => {
    it('should make GET request to orders endpoint', () => {
      service.getMyOrders().subscribe((response) => {
        expect(response).toEqual(mockOrderPage);
      });

      const req = httpMock.expectOne('/api/v1/orders');
      expect(req.request.method).toBe('GET');
      req.flush(mockOrderPage);
    });

    it('should include page parameter', () => {
      service.getMyOrders({ page: 2 }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/orders' && request.params.get('page') === '2'
      );
      req.flush(mockOrderPage);
    });

    it('should include size parameter', () => {
      service.getMyOrders({ size: 20 }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/orders' && request.params.get('size') === '20'
      );
      req.flush(mockOrderPage);
    });

    it('should include status filter', () => {
      service.getMyOrders({ status: 'CONFIRMED' }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/orders' && request.params.get('status') === 'CONFIRMED'
      );
      req.flush(mockOrderPage);
    });

    it('should include createdAfter filter', () => {
      service.getMyOrders({ createdAfter: '2024-01-01T00:00:00Z' }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/orders' && 
        request.params.get('createdAfter') === '2024-01-01T00:00:00Z'
      );
      req.flush(mockOrderPage);
    });

    it('should include createdBefore filter', () => {
      service.getMyOrders({ createdBefore: '2024-12-31T23:59:59Z' }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/orders' && 
        request.params.get('createdBefore') === '2024-12-31T23:59:59Z'
      );
      req.flush(mockOrderPage);
    });

    it('should include multiple parameters', () => {
      service.getMyOrders({
        page: 1,
        size: 10,
        status: 'PAID',
        createdAfter: '2024-01-01T00:00:00Z',
        createdBefore: '2024-12-31T23:59:59Z'
      }).subscribe();

      const req = httpMock.expectOne((request) => {
        const params = request.params;
        return request.url === '/api/v1/orders' &&
          params.get('page') === '1' &&
          params.get('size') === '10' &&
          params.get('status') === 'PAID' &&
          params.get('createdAfter') === '2024-01-01T00:00:00Z' &&
          params.get('createdBefore') === '2024-12-31T23:59:59Z';
      });
      req.flush(mockOrderPage);
    });
  });

  describe('getOrderById', () => {
    it('should make GET request to order detail endpoint', () => {
      const orderId = 'order-123';

      service.getOrderById(orderId).subscribe((order) => {
        expect(order).toEqual(mockOrder);
      });

      const req = httpMock.expectOne(`/api/v1/orders/${orderId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrder);
    });
  });

  describe('cancelOrder', () => {
    it('should make POST request to cancel endpoint', () => {
      const orderId = 'order-123';
      const cancelledOrder: Order = { ...mockOrder, status: 'CANCELLED' };

      service.cancelOrder(orderId).subscribe((order) => {
        expect(order).toEqual(cancelledOrder);
      });

      const req = httpMock.expectOne(`/api/v1/orders/${orderId}/cancel`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(cancelledOrder);
    });
  });
});
