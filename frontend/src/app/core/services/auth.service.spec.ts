import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import {
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  User,
} from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  const mockTokenResponse: TokenResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600
  };

  const mockUser: User = {
    id: 'user-123',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    role: 'USER',
    createdAt: '2024-01-01T00:00:00Z'
  };

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('register', () => {
    it('should make POST request to register endpoint', () => {
      const registerRequest: RegisterRequest = {
        email: 'new@example.com',
        password: 'password123',
        firstName: 'Jane',
        lastName: 'Smith'
      };

      service.register(registerRequest).subscribe((user) => {
        expect(user).toEqual(mockUser);
      });

      const req = httpMock.expectOne('/api/v1/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(registerRequest);
      req.flush(mockUser);
    });
  });

  describe('login', () => {
    it('should make POST request to login endpoint', () => {
      const loginRequest: LoginRequest = {
        email: 'test@example.com',
        password: 'password123'
      };

      service.login(loginRequest).subscribe((response) => {
        expect(response).toEqual(mockTokenResponse);
      });

      const req = httpMock.expectOne('/api/v1/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(loginRequest);
      req.flush(mockTokenResponse);
    });

    it('should store tokens on successful login', () => {
      const loginRequest: LoginRequest = {
        email: 'test@example.com',
        password: 'password123'
      };

      service.login(loginRequest).subscribe(() => {
        expect(localStorage.getItem('refreshToken')).toBe('mock-refresh-token');
        expect(service.getAccessToken()).toBe('mock-access-token');
      });

      const req = httpMock.expectOne('/api/v1/auth/login');
      req.flush(mockTokenResponse);
    });
  });

  describe('logout', () => {
    it('should clear tokens and navigate to login', () => {
      // Set up authenticated state
      localStorage.setItem('refreshToken', 'test-token');
      service.logout();

      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(service.getAccessToken()).toBeNull();
      expect(service.currentUser()).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('refresh', () => {
    it('should make POST request to refresh endpoint with refresh token', () => {
      localStorage.setItem('refreshToken', 'existing-refresh-token');

      service.refresh().subscribe((response) => {
        expect(response).toEqual(mockTokenResponse);
      });

      const req = httpMock.expectOne('/api/v1/auth/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'existing-refresh-token' });
      req.flush(mockTokenResponse);
    });

    it('should return empty observable when no refresh token exists', () => {
      service.refresh().subscribe((response) => {
        expect(response).toEqual({} as TokenResponse);
      });

      httpMock.expectNone('/api/v1/auth/refresh');
    });

    it('should logout on refresh failure', () => {
      localStorage.setItem('refreshToken', 'invalid-token');

      service.refresh().subscribe();

      const req = httpMock.expectOne('/api/v1/auth/refresh');
      req.error(new ProgressEvent('error'));

      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('getMe', () => {
    it('should fetch current user profile', () => {
      service.getMe().subscribe((user) => {
        expect(user).toEqual(mockUser);
        expect(service.currentUser()).toEqual(mockUser);
      });

      const req = httpMock.expectOne('/api/v1/auth/me');
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
  });

  describe('computed properties', () => {
    it('should compute isAuthenticated based on currentUser', () => {
      expect(service.isAuthenticated()).toBe(false);

      service.currentUser.set(mockUser);
      expect(service.isAuthenticated()).toBe(true);
    });

    it('should compute isAdmin based on user role', () => {
      service.currentUser.set(mockUser);
      expect(service.isAdmin()).toBe(false);

      service.currentUser.set({ ...mockUser, role: 'ADMIN' });
      expect(service.isAdmin()).toBe(true);
    });
  });

  describe('getAccessToken', () => {
    it('should return current access token', () => {
      expect(service.getAccessToken()).toBeNull();

      service.login({ email: 'test@example.com', password: 'test' }).subscribe();
      const req = httpMock.expectOne('/api/v1/auth/login');
      req.flush(mockTokenResponse);

      expect(service.getAccessToken()).toBe('mock-access-token');
    });
  });
});
