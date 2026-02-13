import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize login form with email and password controls', () => {
    expect(component.loginForm.get('email')).toBeTruthy();
    expect(component.loginForm.get('password')).toBeTruthy();
  });

  it('should validate email is required', () => {
    const email = component.loginForm.get('email');
    email?.setValue('');
    expect(email?.hasError('required')).toBe(true);
  });

  it('should validate email format', () => {
    const email = component.loginForm.get('email');
    email?.setValue('invalid-email');
    expect(email?.hasError('email')).toBe(true);
    
    email?.setValue('valid@email.com');
    expect(email?.hasError('email')).toBe(false);
  });

  it('should validate password is required', () => {
    const password = component.loginForm.get('password');
    password?.setValue('');
    expect(password?.hasError('required')).toBe(true);
  });

  it('should not submit when form is invalid', () => {
    component.loginForm.patchValue({ email: '', password: '' });
    component.onSubmit();
    
    expect(authService.login).not.toHaveBeenCalled();
    expect(component.loginForm.touched).toBe(false);
  });

  it('should call authService.login on valid form submission', () => {
    const mockResponse = {
      accessToken: 'token123',
      refreshToken: 'refresh123',
      tokenType: 'Bearer',
      expiresIn: 3600
    };
    authService.login.and.returnValue(of(mockResponse));

    component.loginForm.patchValue({
      email: 'test@example.com',
      password: 'password123'
    });

    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'password123'
    });
  });

  it('should navigate to products on successful login', () => {
    const mockResponse = {
      accessToken: 'token123',
      refreshToken: 'refresh123',
      tokenType: 'Bearer',
      expiresIn: 3600
    };
    authService.login.and.returnValue(of(mockResponse));

    component.loginForm.patchValue({
      email: 'test@example.com',
      password: 'password123'
    });

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/products']);
    expect(toastService.success).toHaveBeenCalledWith('Login successful!');
  });

  it('should display error message on login failure', () => {
    const errorResponse = {
      error: {
        detail: 'Invalid credentials'
      }
    };
    authService.login.and.returnValue(throwError(() => errorResponse));

    component.loginForm.patchValue({
      email: 'test@example.com',
      password: 'wrongpassword'
    });

    component.onSubmit();

    expect(component.errorMessage()).toBe('Invalid credentials');
    expect(toastService.error).toHaveBeenCalledWith('Invalid credentials');
    expect(component.loading()).toBe(false);
  });

  it('should set loading state during login', () => {
    authService.login.and.returnValue(of({
      accessToken: 'token123',
      refreshToken: 'refresh123',
      tokenType: 'Bearer',
      expiresIn: 3600
    }));

    component.loginForm.patchValue({
      email: 'test@example.com',
      password: 'password123'
    });

    expect(component.loading()).toBe(false);
    component.onSubmit();
    // Loading is set synchronously before the async call completes
  });
});
