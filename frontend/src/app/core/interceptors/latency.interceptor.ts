import { HttpInterceptorFn } from '@angular/common/http';
import { delay } from 'rxjs';

/** Simule la latence d'un vrai backend tant que les services de données restent mockés. */
export const latencyInterceptor: HttpInterceptorFn = (req, next) => next(req).pipe(delay(150));
