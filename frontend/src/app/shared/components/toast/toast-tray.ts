import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-tray',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'status', 'aria-live': 'polite', class: 'toast-tray' },
  template: `
    @for (toast of toastService.toasts(); track toast.id) {
      <div class="toast-tray__item" [class]="'toast-tray__item--' + toast.type">
        <span>{{ toast.message }}</span>
        <button type="button" class="toast-tray__close" (click)="toastService.dismiss(toast.id)" aria-label="Fermer la notification">×</button>
      </div>
    }
  `,
  styles: `
    :host {
      position: fixed;
      bottom: 20px;
      right: 20px;
      z-index: 100;
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-width: 360px;
    }
    .toast-tray__item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 14px;
      border-radius: 8px;
      background: #0f1729;
      color: #ffffff;
      font-size: 13.5px;
      box-shadow: 0 8px 24px rgba(15, 23, 41, 0.18);
    }
    .toast-tray__item--success { background: #0f6b52; }
    .toast-tray__item--error { background: #922a20; }
    .toast-tray__item--info { background: #0f1729; }
    .toast-tray__close {
      margin-left: auto;
      background: transparent;
      border: none;
      color: inherit;
      font-size: 16px;
      cursor: pointer;
      line-height: 1;
      padding: 2px;
    }
    .toast-tray__close:focus-visible {
      outline: 3px solid rgba(255, 255, 255, 0.5);
      outline-offset: 2px;
      border-radius: 4px;
    }
  `,
})
export class ToastTray {
  protected readonly toastService = inject(ToastService);
}
