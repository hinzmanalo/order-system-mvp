import { HttpInterceptorFn, HttpErrorResponse, HttpEvent, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError, Observable, BehaviorSubject, filter, take } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Skip all token logic when auth bypass is enabled (dev mode)
  if (environment.authBypass) return next(req);

  const authService = inject(AuthService);
  
  // Add access token to request if available
  const token = authService.getAccessToken();
  if (token) {
    console.log('Auth Interceptor: Adding token to request:', req.url);
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  } else {
    console.warn('Auth Interceptor: No token available for request:', req.url);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized errors
      if (error.status === 401 && !req.url.includes('/auth/login') && !req.url.includes('/auth/refresh')) {
        console.log('Auth Interceptor: Handling 401 error for:', req.url);
        return handle401Error(req, next, authService);
      }
      return throwError(() => error);
    })
  );
};

function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService
): Observable<HttpEvent<unknown>> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refresh().pipe(
      switchMap((response) => {
        isRefreshing = false;
        const newToken = response.accessToken;
        refreshTokenSubject.next(newToken);

        // Retry the failed request with new token
        const clonedReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${newToken}`,
          },
        });
        return next(clonedReq);
      }),
      catchError((error) => {
        isRefreshing = false;
        authService.logout();
        return throwError(() => error);
      })
    );
  } else {
    // Wait for token refresh to complete, then retry request
    return refreshTokenSubject.pipe(
      filter((token) => token !== null),
      take(1),
      switchMap((token) => {
        const clonedReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        });
        return next(clonedReq);
      })
    );
  }
}
