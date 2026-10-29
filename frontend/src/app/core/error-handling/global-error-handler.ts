import { ErrorHandler, Injectable, inject } from '@angular/core';
import { ToastService } from '@shared/components/toast/toast.service';

const CHUNK_RELOAD_KEY = 'gpdi-chunk-reload';
const CHUNK_ERROR_PATTERN = /Failed to fetch dynamically imported module|Importing a module script failed|error loading dynamically imported module|ChunkLoadError/i;

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly toast = inject(ToastService);

  handleError(error: unknown): void {
    if (this.estErreurChargementChunk(error) && this.rechargerUneFois()) {
      return;
    }
    console.error(error);
    this.toast.show({ type: 'error', message: 'Une erreur inattendue est survenue.' });
  }

  private estErreurChargementChunk(error: unknown): boolean {
    const message = error instanceof Error ? error.message : String((error as { message?: unknown })?.message ?? error);
    return CHUNK_ERROR_PATTERN.test(message);
  }

  // Garde anti-boucle : un seul rechargement automatique par période de 10 s.
  private rechargerUneFois(): boolean {
    try {
      const dernier = Number(sessionStorage.getItem(CHUNK_RELOAD_KEY) ?? 0);
      if (Date.now() - dernier < 10_000) return false;
      sessionStorage.setItem(CHUNK_RELOAD_KEY, String(Date.now()));
    } catch {
      return false;
    }
    window.location.reload();
    return true;
  }
}
