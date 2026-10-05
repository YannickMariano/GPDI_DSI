import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Button } from '@shared/components/button/button';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Button],
  host: { class: 'empty-state' },
  template: `
    <div class="empty-state__icon" aria-hidden="true">{{ icon() }}</div>
    <div class="empty-state__title">{{ title() }}</div>
    @if (description()) {
      <p class="empty-state__description">{{ description() }}</p>
    }
    @if (actionLabel()) {
      <button app-button variant="primary" type="button" (click)="action.emit()">{{ actionLabel() }}</button>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      text-align: center;
      padding: 40px 20px;
      color: var(--color-text-tertiary);
    }
    .empty-state__icon {
      font-size: 24px;
      color: var(--color-text-quaternary);
    }
    .empty-state__title {
      font-size: 14px;
      font-weight: 700;
      color: var(--color-ink);
    }
    .empty-state__description {
      margin: 0;
      font-size: 13px;
      line-height: 1.5;
      max-width: 320px;
    }
  `,
})
export class EmptyState {
  readonly icon = input<string>('◌');
  readonly title = input.required<string>();
  readonly description = input<string | undefined>(undefined);
  readonly actionLabel = input<string | undefined>(undefined);
  readonly action = output<void>();
}
