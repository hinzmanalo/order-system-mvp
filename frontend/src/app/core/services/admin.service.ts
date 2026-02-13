import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductPage, ProductRequest, UpdateProductStatusRequest } from '../models/product.model';
import { InventoryResponse, InventoryPage, SetStockRequest, AdjustStockRequest } from '../models/inventory.model';
import { User, UserPage, UpdateRoleRequest } from '../models/user.model';
import { Order, OrderPage } from '../models/order.model';

/**
 * Service for admin operations including product, inventory, user, and order management
 */
@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/v1/admin';

  // ========== Product Management ==========

  /**
   * Creates a new product
   * 
   * @param request the product creation request
   * @returns Observable of the created product
   */
  createProduct(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(`${this.API_URL}/products`, request);
  }

  /**
   * Updates an existing product
   * 
   * @param id the product ID to update
   * @param request the product update request
   * @returns Observable of the updated product
   */
  updateProduct(id: string, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.API_URL}/products/${id}`, request);
  }

  /**
   * Updates the active status of a product
   * 
   * @param id the product ID
   * @param active the new active status
   * @returns Observable of the updated product
   */
  updateProductStatus(id: string, active: boolean): Observable<Product> {
    const request: UpdateProductStatusRequest = { active };
    return this.http.patch<Product>(`${this.API_URL}/products/${id}/status`, request);
  }

  // ========== Inventory Management ==========

  /**
   * Retrieves paginated inventory data for all products
   * 
   * @param params optional query parameters for pagination
   * @returns Observable of paginated inventory
   */
  getInventory(params: { page?: number; size?: number } = {}): Observable<InventoryPage> {
    let httpParams = new HttpParams();

    if (params.page !== undefined && params.page !== null) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined && params.size !== null) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<InventoryPage>(`${this.API_URL}/inventory`, { params: httpParams });
  }

  /**
   * Retrieves inventory for a specific product
   * 
   * @param productId the product ID
   * @returns Observable of the inventory response
   */
  getInventoryByProductId(productId: string): Observable<InventoryResponse> {
    return this.http.get<InventoryResponse>(`${this.API_URL}/inventory/${productId}`);
  }

  /**
   * Sets the absolute stock quantity for a product
   * 
   * @param productId the product ID
   * @param quantity the new absolute quantity
   * @returns Observable of the updated inventory
   */
  setStock(productId: string, quantity: number): Observable<InventoryResponse> {
    const request: SetStockRequest = { quantity };
    return this.http.put<InventoryResponse>(`${this.API_URL}/inventory/${productId}`, request);
  }

  /**
   * Adjusts stock by a relative amount (positive or negative)
   * 
   * @param productId the product ID
   * @param adjustment the amount to add or subtract from current stock
   * @returns Observable of the updated inventory
   */
  adjustStock(productId: string, adjustment: number): Observable<InventoryResponse> {
    const request: AdjustStockRequest = { adjustment };
    return this.http.patch<InventoryResponse>(`${this.API_URL}/inventory/${productId}/adjust`, request);
  }

  // ========== User Management ==========

  /**
   * Retrieves paginated list of all users
   * 
   * @param params optional query parameters for pagination
   * @returns Observable of paginated users
   */
  getUsers(params: { page?: number; size?: number } = {}): Observable<UserPage> {
    let httpParams = new HttpParams();

    if (params.page !== undefined && params.page !== null) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined && params.size !== null) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<UserPage>(`${this.API_URL}/users`, { params: httpParams });
  }

  /**
   * Retrieves a single user by ID
   * 
   * @param id the user ID
   * @returns Observable of the user
   */
  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/users/${id}`);
  }

  /**
   * Updates the role of a user
   * 
   * @param id the user ID
   * @param role the new role (USER or ADMIN)
   * @returns Observable of the updated user
   */
  updateUserRole(id: string, role: string): Observable<User> {
    const request: UpdateRoleRequest = { role };
    return this.http.put<User>(`${this.API_URL}/users/${id}/role`, request);
  }

  // ========== Order Management ==========

  /**
   * Retrieves all orders across all users with optional filters
   * 
   * @param params optional query parameters for filtering and pagination
   * @returns Observable of paginated orders
   */
  getAllOrders(params: {
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

    return this.http.get<OrderPage>(`${this.API_URL}/orders`, { params: httpParams });
  }

  /**
   * Retrieves a single order by ID (any user's order)
   * 
   * @param id the order ID
   * @returns Observable of the order
   */
  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.API_URL}/orders/${id}`);
  }

  /**
   * Cancels any order
   * 
   * @param id the order ID to cancel
   * @returns Observable of the cancelled order
   */
  cancelOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.API_URL}/orders/${id}/cancel`, {});
  }
}
