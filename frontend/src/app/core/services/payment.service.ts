import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment, PaymentRequest } from '../models/payment.model';

/**
 * Service for managing payment operations
 */
@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/v1/orders';

  /**
   * Processes a payment for an order
   * 
   * @param orderId the order ID to process payment for
   * @param request the payment request with amount
   * @returns Observable of the payment result
   */
  processPayment(orderId: string, request: PaymentRequest): Observable<Payment> {
    // Generate unique idempotency key to prevent duplicate payment processing
    const idempotencyKey = crypto.randomUUID();
    
    const headers = new HttpHeaders({
      'Idempotency-Key': idempotencyKey
    });

    return this.http.post<Payment>(
      `${this.API_URL}/${orderId}/payments`,
      request,
      { headers }
    );
  }

  /**
   * Retrieves all payments for an order
   * 
   * @param orderId the order ID
   * @returns Observable of payment array
   */
  getPayments(orderId: string): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.API_URL}/${orderId}/payments`);
  }
}
