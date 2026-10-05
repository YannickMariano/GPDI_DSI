import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

let nextId = 1;

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  show(toast: { type: ToastType; message: string; durationMs?: number }): void {
    const id = nextId++;
    this._toasts.update((list) => [...list, { id, type: toast.type, message: toast.message }]);
    setTimeout(() => this.dismiss(id), toast.durationMs ?? 4000);
  }

  dismiss(id: number): void {
    this._toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
