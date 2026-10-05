import { ErrorHandler, Injectable, inject } from '@angular/core';
import { ToastService } from '@shared/components/toast/toast.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly toast = inject(ToastService);

  handleError(error: unknown): void {
    console.error(error);
    this.toast.show({ type: 'error', message: 'Une erreur inattendue est survenue.' });
  }
}
