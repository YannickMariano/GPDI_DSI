import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { criticiteRisque } from '@core/models/risk.model';
import { RisqueProjet } from '@core/services/risques-projet.service';

const CRITICITE_STYLE: Record<string, { bg: string; text: string }> = {
  critique: { bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)' },
  eleve: { bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)' },
  modere: { bg: '#fdf8e3', text: '#8a6d1a' },
  faible: { bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
};

@Component({
  selector: 'app-risk-matrix',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'risk-matrix' },
  templateUrl: './risk-matrix.html',
  styleUrl: './risk-matrix.css',
})
export class RiskMatrix {
  readonly risks = input.required<RisqueProjet[]>();
  protected readonly niveaux = [5, 4, 3, 2, 1];

  protected readonly cellFor = computed(() => {
    const actifs = this.risks().filter((r) => r.statut !== 'clos');
    return (probabilite: number, impact: number) => {
      const matches = actifs.filter((r) => r.probabilite === probabilite && r.impact === impact);
      const criticite = criticiteRisque(probabilite, impact);
      return { codes: matches.map((r) => r.code).join(', '), style: CRITICITE_STYLE[criticite] };
    };
  });

  protected readonly synthese = computed(() => {
    const actifs = this.risks().filter((r) => r.statut !== 'clos');
    const counts: Record<'critique' | 'eleve' | 'modere' | 'faible', number> = { critique: 0, eleve: 0, modere: 0, faible: 0 };
    for (const r of actifs) counts[criticiteRisque(r.probabilite, r.impact)]++;
    return counts;
  });

  protected readonly criticiteStyle = CRITICITE_STYLE;
}
