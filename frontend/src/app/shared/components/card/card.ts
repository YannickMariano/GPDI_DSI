import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'app-card' },
  template: `<ng-content />`,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 16px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-card);
      padding: 20px 22px;
    }
  `,
})
export class Card {}
