import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import {
  User,
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  RefreshRequest,
} from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly API_URL = '/api/v1/auth';
  
  private accessToken: string | null = null;
  
  // Signals for reactive state
  currentUser = signal<User | null>(null);
  isAuthenticated = computed(() => !!this.currentUser());
  isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  constructor() {
    // Attempt silent authentication on service initialization
    this.initializeAuth();
  }

  /**
   * Registers a new user account
   */
  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.API_URL}/register`, request);
  }

  /**
   * Authenticates user and stores tokens
   */
  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.API_URL}/login`, request).pipe(
      tap((response) => {
        this.storeTokens(response);
        this.decodeAndSetUser(response.accessToken);
      })
    );
  }

  /**
   * Logs out the current user
   */
  logout(): void {
    this.accessToken = null;
    localStorage.removeItem('refreshToken');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  /**
   * Refreshes the access token using the refresh token
   */
  refresh(): Observable<TokenResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      return of({} as TokenResponse);
    }

    const request: RefreshRequest = { refreshToken };
    return this.http.post<TokenResponse>(`${this.API_URL}/refresh`, request).pipe(
      tap((response) => {
        this.storeTokens(response);
        this.decodeAndSetUser(response.accessToken);
      }),
      catchError(() => {
        this.logout();
        return of({} as TokenResponse);
      })
    );
  }

  /**
   * Fetches the current user's profile
   */
  getMe(): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/me`).pipe(
      tap((user) => this.currentUser.set(user))
    );
  }

  /**
   * Returns the current access token
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }

  /**
   * Checks if the access token is expired
   */
  isTokenExpired(): boolean {
    if (!this.accessToken) {
      return true;
    }

    try {
      const payload = this.decodeToken(this.accessToken);
      const exp = payload.exp * 1000; // Convert to milliseconds
      return Date.now() >= exp;
    } catch {
      return true;
    }
  }

  /**
   * Stores tokens in memory and localStorage
   */
  private storeTokens(response: TokenResponse): void {
    this.accessToken = response.accessToken;
    localStorage.setItem('refreshToken', response.refreshToken);
  }

  /**
   * Decodes JWT token and extracts user information
   */
  private decodeAndSetUser(token: string): void {
    try {
      const payload = this.decodeToken(token);
      // JWT payload typically contains user info - adjust based on your backend
      const user: User = {
        id: payload.sub || payload.userId,
        email: payload.email,
        firstName: payload.firstName || '',
        lastName: payload.lastName || '',
        role: payload.role || 'USER',
        createdAt: '',
      };
      this.currentUser.set(user);
    } catch (error) {
      console.error('Failed to decode token', error);
    }
  }

  /**
   * Decodes a JWT token
   */
  private decodeToken(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) {
      throw new Error('Invalid token format');
    }

    const payload = parts[1];
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decoded);
  }

  /**
   * Initializes authentication on service construction
   */
  private initializeAuth(): void {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      this.refresh().subscribe({
        next: () => {
          // Get full user profile after successful refresh
          this.getMe().subscribe();
        },
        error: () => {
          // Silent failure - user will need to log in again
          localStorage.removeItem('refreshToken');
        },
      });
    }
  }
}
