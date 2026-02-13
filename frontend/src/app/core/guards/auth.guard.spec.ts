import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

describe('authGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      isAuthenticated: jasmine.createSpy().and.returnValue(false)
    });
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should allow access when user is authenticated', () => {
    Object.defineProperty(authService, 'isAuthenticated', {
      get: () => () => true
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/products' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access when user is not authenticated', () => {
    Object.defineProperty(authService, 'isAuthenticated', {
      get: () => () => false
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/products' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(result).toBe(false);
  });

  it('should redirect to login with return url when not authenticated', () => {
    Object.defineProperty(authService, 'isAuthenticated', {
      get: () => () => false
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/orders' } as RouterStateSnapshot;

    TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(router.navigate).toHaveBeenCalledWith(
      ['/login'],
      { queryParams: { returnUrl: '/orders' } }
    );
  });

  it('should redirect to login with correct return url for different routes', () => {
    Object.defineProperty(authService, 'isAuthenticated', {
      get: () => () => false
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/admin/products' } as RouterStateSnapshot;

    TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(router.navigate).toHaveBeenCalledWith(
      ['/login'],
      { queryParams: { returnUrl: '/admin/products' } }
    );
  });
});
