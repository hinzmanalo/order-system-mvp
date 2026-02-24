import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

export const adminGuard: CanActivateFn = (route, state) => {
  if (environment.authBypass) return true;

  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  // Redirect to products page if not admin
  router.navigate(['/products']);
  return false;
};
