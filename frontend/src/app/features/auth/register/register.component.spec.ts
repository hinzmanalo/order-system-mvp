import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['register']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const toastServiceSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize register form with all required fields', () => {
    expect(component.registerForm.get('email')).toBeTruthy();
    expect(component.registerForm.get('password')).toBeTruthy();
    expect(component.registerForm.get('firstName')).toBeTruthy();
    expect(component.registerForm.get('lastName')).toBeTruthy();
  });

  it('should validate email is required and valid', () => {
    const email = component.registerForm.get('email');
    
    email?.setValue('');
    expect(email?.hasError('required')).toBe(true);
    
    email?.setValue('invalid-email');
    expect(email?.hasError('email')).toBe(true);
    
    email?.setValue('valid@email.com');
    expect(email?.valid).toBe(true);
  });

  it('should validate password minimum length', () => {
    const password = component.registerForm.get('password');
    
    password?.setValue('12345');
    expect(password?.hasError('minlength')).toBe(true);
    
    password?.setValue('123456');
    expect(password?.hasError('minlength')).toBe(false);
  });

  it('should validate firstName is required', () => {
    const firstName = component.registerForm.get('firstName');
    
    firstName?.setValue('');
    expect(firstName?.hasError('required')).toBe(true);
    
    firstName?.setValue('John');
    expect(firstName?.valid).toBe(true);
  });

  it('should validate lastName is required', () => {
    const lastName = component.registerForm.get('lastName');
    
    lastName?.setValue('');
    expect(lastName?.hasError('required')).toBe(true);
    
    lastName?.setValue('Doe');
    expect(lastName?.valid).toBe(true);
  });

  it('should not submit when form is invalid', () => {
    component.registerForm.patchValue({
      email: 'invalid',
      password: '123',
      firstName: '',
      lastName: ''
    });
    
    component.onSubmit();
    
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('should call authService.register on valid form submission', () => {
    const mockResponse = {
      id: '123-456',
      email: 'test@example.com',
      firstName: 'John',
      lastName: 'Doe',
      role: 'USER' as const,
      createdAt: '2024-01-01T00:00:00Z'
    };
    authService.register.and.returnValue(of(mockResponse));

    component.registerForm.patchValue({
      email: 'test@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe'
    });

    component.onSubmit();

    expect(authService.register).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe'
    });
  });

  it('should navigate to login on successful registration', () => {
    const mockResponse = {
      id: '123-456',
      email: 'test@example.com',
      firstName: 'John',
      lastName: 'Doe',
      role: 'USER' as const,
      createdAt: '2024-01-01T00:00:00Z'
    };
    authService.register.and.returnValue(of(mockResponse));

    component.registerForm.patchValue({
      email: 'test@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe'
    });

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(toastService.success).toHaveBeenCalledWith('Registration successful! Please sign in.');
  });

  it('should display error message on registration failure', () => {
    const errorResponse = {
      error: {
        detail: 'Email already exists'
      }
    };
    authService.register.and.returnValue(throwError(() => errorResponse));

    component.registerForm.patchValue({
      email: 'existing@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe'
    });

    component.onSubmit();

    expect(component.errorMessage()).toBe('Email already exists');
    expect(toastService.error).toHaveBeenCalledWith('Email already exists');
    expect(component.loading()).toBe(false);
  });

  it('should use default error message when detail is not available', () => {
    const errorResponse = { error: {} };
    authService.register.and.returnValue(throwError(() => errorResponse));

    component.registerForm.patchValue({
      email: 'test@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe'
    });

    component.onSubmit();

    expect(component.errorMessage()).toBe('Registration failed. Please try again.');
  });
});
