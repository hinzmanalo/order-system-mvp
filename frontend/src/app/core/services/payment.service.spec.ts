import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';
import { Payment, PaymentRequest } from '../models/payment.model';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  const mockPayment: Payment = {
    id: 'payment-123',
    orderId: 'order-123',
    amount: 99.99,
    status: 'SUCCESS',
    gatewayReference: 'gateway-ref-123',
    createdAt: '2024-01-01T12:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentService]
    });

    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('processPayment', () => {
    it('should make POST request to payment endpoint', () => {
      const orderId = 'order-123';
      const paymentRequest: PaymentRequest = {
        amount: 99.99
      };

      service.processPayment(orderId, paymentRequest).subscribe((payment) => {
        expect(payment).toEqual(mockPayment);
      });

      const req = httpMock.expectOne(`/api/v1/orders/${orderId}/payments`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(paymentRequest);
      req.flush(mockPayment);
    });

    it('should include Idempotency-Key header', () => {
      const orderId = 'order-123';
      const paymentRequest: PaymentRequest = {
        amount: 99.99
      };

      service.processPayment(orderId, paymentRequest).subscribe();

      const req = httpMock.expectOne(`/api/v1/orders/${orderId}/payments`);
      expect(req.request.headers.has('Idempotency-Key')).toBe(true);
      expect(req.request.headers.get('Idempotency-Key')).toBeTruthy();
      
      // Verify it's a valid UUID format
      const idempotencyKey = req.request.headers.get('Idempotency-Key');
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
      expect(uuidRegex.test(idempotencyKey!)).toBe(true);

      req.flush(mockPayment);
    });

    it('should generate unique Idempotency-Key for each request', () => {
      const orderId = 'order-123';
      const paymentRequest: PaymentRequest = {
        amount: 99.99
      };

      // First request
      service.processPayment(orderId, paymentRequest).subscribe();
      const req1 = httpMock.expectOne(`/api/v1/orders/${orderId}/payments`);
      const key1 = req1.request.headers.get('Idempotency-Key');
      req1.flush(mockPayment);

      // Second request
      service.processPayment(orderId, paymentRequest).subscribe();
      const req2 = httpMock.expectOne(`/api/v1/orders/${orderId}/payments`);
      const key2 = req2.request.headers.get('Idempotency-Key');
      req2.flush(mockPayment);

      expect(key1).not.toEqual(key2);
    });
  });

  describe('getPayments', () => {
    it('should make GET request to payments endpoint', () => {
      const orderId = 'order-123';
      const mockPayments: Payment[] = [mockPayment];

      service.getPayments(orderId).subscribe((payments) => {
        expect(payments).toEqual(mockPayments);
      });

      const req = httpMock.expectOne(`/api/v1/orders/${orderId}/payments`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPayments);
    });

    it('should return empty array when no payments exist', () => {
      const orderId = 'order-123';

      service.getPayments(orderId).subscribe((payments) => {
        expect(payments).toEqual([]);
      });

      const req = httpMock.expectOne(`/api/v1/orders/${orderId}/payments`);
      req.flush([]);
    });
  });
});
