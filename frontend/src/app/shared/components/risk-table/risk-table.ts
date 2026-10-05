import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { criticiteRisque } from '@core/models/risk.model';
import { RisqueProjet } from '@core/services/risques-projet.service';
import { Person } from '@core/models/person.model';
import { Avatar } from '@shared/components/avatar/avatar';
import { EmptyState } from '@shared/components/empty-state/empty-state';

const CRITICITE_LABEL: Record<string, { label: string; bg: string; text: string }> = {
  critique: { label: 'Critique', bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)' },
  eleve: { label: 'Élevé', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)' },
  modere: { label: 'Modéré', bg: '#fdf8e3', text: '#8a6d1a' },
  faible: { label: 'Faible', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
};

@Component({
  selector: 'app-risk-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, EmptyState],
  templateUrl: './risk-table.html',
  styleUrl: './risk-table.css',
})
export class RiskTable {
  readonly risks = input.required<RisqueProjet[]>();
  readonly people = input.required<Person[]>();
  protected readonly criticiteLabel = CRITICITE_LABEL;
  protected readonly criticiteRisque = criticiteRisque;

  porteur(id: string): Person | undefined {
    return this.people().find((p) => p.id === id);
  }
}
