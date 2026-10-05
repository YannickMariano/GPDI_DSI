import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '@shared/components/toast/toast.service';
import { environment } from '@environments/environment';

/** À activer sur une requête d'arrière-plan (actualisation automatique) dont l'échec ne doit pas déranger l'utilisateur. */
export const SANS_TOAST_ERREUR = new HttpContextToken<boolean>(() => false);

/** Traduit les erreurs de l'API GPDI (`{ message }`, cf. `ErreurResponse` backend) en toast. Le 401 est laissé à la charge de l'authentification (rafraîchissement de session). */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && req.url.startsWith(environment.apiUrl) && error.status !== 401 && !req.context.get(SANS_TOAST_ERREUR)) {
        const message = typeof error.error?.message === 'string' ? error.error.message : 'Une erreur est survenue. Veuillez réessayer.';
        toast.show({ type: 'error', message });
      }
      return throwError(() => error);
    }),
  );
};
