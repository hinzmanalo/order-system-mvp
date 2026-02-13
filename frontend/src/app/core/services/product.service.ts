import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductPage } from '../models/product.model';

/**
 * Service for managing product operations
 */
@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/v1/products';

  /**
   * Retrieves a paginated list of products with optional filters
   */
  getProducts(params: {
    page?: number;
    size?: number;
    name?: string;
    minPrice?: number;
    maxPrice?: number;
    sort?: string;
  }): Observable<ProductPage> {
    let httpParams = new HttpParams();

    if (params.page !== undefined && params.page !== null) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined && params.size !== null) {
      httpParams = httpParams.set('size', params.size.toString());
    }
    if (params.name) {
      httpParams = httpParams.set('name', params.name);
    }
    if (params.minPrice !== undefined && params.minPrice !== null) {
      httpParams = httpParams.set('minPrice', params.minPrice.toString());
    }
    if (params.maxPrice !== undefined && params.maxPrice !== null) {
      httpParams = httpParams.set('maxPrice', params.maxPrice.toString());
    }
    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }

    return this.http.get<ProductPage>(this.API_URL, { params: httpParams });
  }

  /**
   * Retrieves a single product by ID
   */
  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.API_URL}/${id}`);
  }
}
