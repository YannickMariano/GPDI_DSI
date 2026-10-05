import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Priorite } from '@core/models/task.model';

const PRIORITY_CONFIG: Record<Priorite, { label: string; bg: string; text: string }> = {
  critique: { label: 'CRITIQUE', bg: 'var(--priority-critique-bg)', text: 'var(--priority-critique-text)' },
  haute: { label: 'HAUTE', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)' },
  normale: { label: 'NORMALE', bg: 'var(--priority-normale-bg)', text: 'var(--priority-normale-text)' },
  basse: { label: 'BASSE', bg: 'var(--priority-basse-bg)', text: 'var(--priority-basse-text)' },
};

@Component({
  selector: 'app-priority-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'priority-badge',
    '[style.background]': 'config().bg',
    '[style.color]': 'config().text',
  },
  template: `{{ config().label }}`,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      padding: 3px 9px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.02em;
      width: fit-content;
    }
  `,
})
export class PriorityBadge {
  readonly priorite = input.required<Priorite>();
  protected readonly config = computed(() => PRIORITY_CONFIG[this.priorite()]);
}
