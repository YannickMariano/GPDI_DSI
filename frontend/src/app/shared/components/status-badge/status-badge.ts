import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { StatutTache } from '@core/models/task.model';

const STATUS_CONFIG: Record<StatutTache, { label: string; bg: string; text: string; dot: string }> = {
  a_faire: { label: 'À faire', bg: 'var(--status-a-faire-bg)', text: 'var(--status-a-faire-text)', dot: 'var(--status-a-faire-dot)' },
  en_cours: { label: 'En cours', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)', dot: 'var(--status-en-cours-dot)' },
  en_test: { label: 'En test', bg: 'var(--status-en-test-bg)', text: 'var(--status-en-test-text)', dot: 'var(--status-en-test-dot)' },
  bloquee: { label: 'Bloquée', bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)', dot: 'var(--status-bloquee-dot)' },
  terminee: { label: 'Terminée', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)', dot: 'var(--status-terminee-dot)' },
  reportee: { label: 'Reportée', bg: 'var(--status-reportee-bg)', text: 'var(--status-reportee-text)', dot: 'var(--status-reportee-dot)' },
};

@Component({
  selector: 'app-status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'status-badge',
    '[style.background]': 'config().bg',
    '[style.color]': 'config().text',
  },
  template: `
    <span class="status-badge__dot" [style.background]="config().dot"></span>
    <span>{{ config().label }}</span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 20px;
      font-size: 12.5px;
      font-weight: 600;
      width: fit-content;
    }
    .status-badge__dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex: 0 0 auto;
    }
  `,
})
export class StatusBadge {
  readonly statut = input.required<StatutTache>();
  protected readonly config = computed(() => STATUS_CONFIG[this.statut()]);
}
