import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'destructive' | 'ghost';

@Component({
  selector: 'button[app-button]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'app-button',
    '[class]': '"app-button--" + variant()',
    '[attr.disabled]': 'disabled() ? true : null',
  },
  template: `<ng-content />`,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 9px 16px;
      border-radius: var(--radius-control);
      border: 1px solid transparent;
      font-family: var(--font-ui);
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s ease;
    }
    :host(:disabled) {
      cursor: not-allowed;
      opacity: 0.55;
    }
    :host(.app-button--primary) {
      background: var(--color-action);
      color: #ffffff;
    }
    :host(.app-button--primary:hover:not(:disabled)) {
      background: var(--color-action-hover);
    }
    :host(.app-button--secondary) {
      background: var(--color-surface);
      color: #344054;
      border-color: var(--color-border-strong);
    }
    :host(.app-button--secondary:hover:not(:disabled)) {
      background: var(--color-surface-muted);
    }
    :host(.app-button--destructive) {
      background: var(--color-error-bg);
      color: var(--color-error);
      border-color: var(--color-error-border);
    }
    :host(.app-button--destructive:hover:not(:disabled)) {
      background: #fbdedb;
    }
    :host(.app-button--ghost) {
      background: transparent;
      color: var(--color-text-secondary);
    }
    :host(.app-button--ghost:hover:not(:disabled)) {
      background: var(--color-surface-alt);
    }
  `,
})
export class Button {
  readonly variant = input<ButtonVariant>('primary');
  readonly disabled = input<boolean>(false);
}
