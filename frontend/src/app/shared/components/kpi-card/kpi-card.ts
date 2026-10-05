import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-kpi-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'kpi-card' },
  template: `
    <span class="kpi-card__label">{{ label() }}</span>
    <span class="kpi-card__value">
      {{ value() }}
      @if (unite()) {
        <span class="kpi-card__unite">{{ unite() }}</span>
      }
    </span>
    @if (delta()) {
      <span class="kpi-card__delta" [style.color]="deltaColor()">{{ delta() }}</span>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 6px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-card);
      padding: 18px 20px;
    }
    .kpi-card__label {
      font-size: 12.5px;
      color: var(--color-text-tertiary);
      font-weight: 600;
    }
    .kpi-card__value {
      font-size: 26px;
      font-weight: 800;
      letter-spacing: -0.02em;
      color: var(--color-ink);
    }
    .kpi-card__unite {
      font-size: 13px;
      font-weight: 500;
      color: var(--color-text-tertiary);
      margin-left: 2px;
    }
    .kpi-card__delta {
      font-size: 12.5px;
      font-weight: 600;
    }
  `,
})
export class KpiCard {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly unite = input<string | undefined>(undefined);
  readonly delta = input<string | undefined>(undefined);
  readonly deltaColor = input<string>('var(--color-success)');
}
