import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { environment } from '@environments/environment';

/** Attache le jeton d'accès courant et, sur un 401 de l'API GPDI, tente un rafraîchissement Keycloak avant de rejouer la requête une fois. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.sessionToken();
  const authorized = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      const rejouable = token && error instanceof HttpErrorResponse && error.status === 401 && req.url.startsWith(environment.apiUrl);
      if (!rejouable) {
        return throwError(() => error);
      }

      return from(authService.refreshSession()).pipe(
        switchMap((rafraichi) => {
          if (!rafraichi) {
            return throwError(() => error);
          }
          return next(req.clone({ setHeaders: { Authorization: `Bearer ${authService.sessionToken()}` } }));
        }),
      );
    }),
  );
};
