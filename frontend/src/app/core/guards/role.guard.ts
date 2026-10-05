import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { Role } from '@core/models/role.model';

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const user = authService.currentUser();

    if (user && roles.includes(user.role)) {
      return true;
    }

    return router.createUrlTree(['/acces-refuse']);
  };
}
