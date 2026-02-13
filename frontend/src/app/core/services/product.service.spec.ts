import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { Product, ProductPage } from '../models/product.model';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  const mockProduct: Product = {
    id: '1',
    sku: 'PROD-001',
    name: 'Test Product',
    description: 'Test Description',
    price: 99.99,
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  };

  const mockProductPage: ProductPage = {
    content: [mockProduct],
    totalPages: 1,
    totalElements: 1,
    number: 0,
    size: 20,
    first: true,
    last: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductService]
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getProducts', () => {
    it('should make GET request to products endpoint', () => {
      service.getProducts({}).subscribe((response) => {
        expect(response).toEqual(mockProductPage);
      });

      const req = httpMock.expectOne('/api/v1/products');
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should include page parameter in request', () => {
      service.getProducts({ page: 2 }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/products' && request.params.get('page') === '2'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should include size parameter in request', () => {
      service.getProducts({ size: 10 }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/products' && request.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should include name filter in request', () => {
      service.getProducts({ name: 'laptop' }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/products' && request.params.get('name') === 'laptop'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should include minPrice filter in request', () => {
      service.getProducts({ minPrice: 50 }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/products' && request.params.get('minPrice') === '50'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should include maxPrice filter in request', () => {
      service.getProducts({ maxPrice: 200 }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/products' && request.params.get('maxPrice') === '200'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should include sort parameter in request', () => {
      service.getProducts({ sort: 'price,desc' }).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/v1/products' && request.params.get('sort') === 'price,desc'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should include multiple parameters in request', () => {
      service.getProducts({
        page: 1,
        size: 12,
        name: 'test',
        minPrice: 10,
        maxPrice: 100,
        sort: 'name,asc'
      }).subscribe();

      const req = httpMock.expectOne((request) => {
        const params = request.params;
        return request.url === '/api/v1/products' &&
          params.get('page') === '1' &&
          params.get('size') === '12' &&
          params.get('name') === 'test' &&
          params.get('minPrice') === '10' &&
          params.get('maxPrice') === '100' &&
          params.get('sort') === 'name,asc';
      });
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });

    it('should not include undefined parameters in request', () => {
      service.getProducts({ name: undefined, minPrice: undefined }).subscribe();

      const req = httpMock.expectOne((request) => {
        const params = request.params;
        return request.url === '/api/v1/products' &&
          !params.has('name') &&
          !params.has('minPrice');
      });
      expect(req.request.method).toBe('GET');
      req.flush(mockProductPage);
    });
  });

  describe('getProductById', () => {
    it('should make GET request to product detail endpoint', () => {
      const productId = '123-456';

      service.getProductById(productId).subscribe((product) => {
        expect(product).toEqual(mockProduct);
      });

      const req = httpMock.expectOne(`/api/v1/products/${productId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProduct);
    });
  });
});
