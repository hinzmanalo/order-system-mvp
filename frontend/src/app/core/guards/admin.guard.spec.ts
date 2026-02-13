import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { adminGuard } from './admin.guard';
import { AuthService } from '../services/auth.service';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

describe('adminGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      isAdmin: jasmine.createSpy().and.returnValue(false)
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

  it('should allow access when user is admin', () => {
    Object.defineProperty(authService, 'isAdmin', {
      get: () => () => true
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/admin/products' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => adminGuard(route, state));

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access when user is not admin', () => {
    Object.defineProperty(authService, 'isAdmin', {
      get: () => () => false
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/admin/products' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => adminGuard(route, state));

    expect(result).toBe(false);
  });

  it('should redirect to products page when not admin', () => {
    Object.defineProperty(authService, 'isAdmin', {
      get: () => () => false
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/admin/orders' } as RouterStateSnapshot;

    TestBed.runInInjectionContext(() => adminGuard(route, state));

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('should redirect to products for any admin route when not admin', () => {
    Object.defineProperty(authService, 'isAdmin', {
      get: () => () => false
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/admin/users' } as RouterStateSnapshot;

    TestBed.runInInjectionContext(() => adminGuard(route, state));

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });
});
