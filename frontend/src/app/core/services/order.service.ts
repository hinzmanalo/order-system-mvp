import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, OrderPage, CreateOrderRequest } from '../models/order.model';

/**
 * Service for managing order operations
 */
@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/v1/orders';

  /**
   * Creates a new order
   * 
   * @param request the order creation request with items
   * @returns Observable of the created order
   */
  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.API_URL, request);
  }

  /**
   * Retrieves the current user's orders with optional filters
   * 
   * @param params optional query parameters for filtering and pagination
   * @returns Observable of paginated orders
   */
  getMyOrders(params: {
    page?: number;
    size?: number;
    status?: string;
    createdAfter?: string;
    createdBefore?: string;
  } = {}): Observable<OrderPage> {
    let httpParams = new HttpParams();

    if (params.page !== undefined && params.page !== null) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined && params.size !== null) {
      httpParams = httpParams.set('size', params.size.toString());
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.createdAfter) {
      httpParams = httpParams.set('createdAfter', params.createdAfter);
    }
    if (params.createdBefore) {
      httpParams = httpParams.set('createdBefore', params.createdBefore);
    }

    console.log('OrderService: Fetching orders with params', params);
    return this.http.get<OrderPage>(this.API_URL, { params: httpParams });
  }

  /**
   * Retrieves a single order by ID
   * 
   * @param id the order ID
   * @returns Observable of the order
   */
  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.API_URL}/${id}`);
  }

  /**
   * Cancels an order
   * 
   * @param id the order ID to cancel
   * @returns Observable of the cancelled order
   */
  cancelOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.API_URL}/${id}/cancel`, {});
  }
}
