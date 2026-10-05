import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RisqueProjet, RisquesProjetService } from '@core/services/risques-projet.service';
import { PeopleService } from '@core/services/people.service';
import { StatutRisque, criticiteRisque } from '@core/models/risk.model';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { ToastService } from '@shared/components/toast/toast.service';
import { formatDateCourteFr } from '@shared/utils/format.util';

const CRITICITE_LABEL: Record<string, { label: string; bg: string; text: string }> = {
  critique: { label: 'Critique', bg: 'var(--status-bloquee-bg)', text: 'var(--status-bloquee-text)' },
  eleve: { label: 'Élevé', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)' },
  modere: { label: 'Modéré', bg: '#fdf8e3', text: '#8a6d1a' },
  faible: { label: 'Faible', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
};

type FiltreStatut = 'tous' | StatutRisque;

const FILTRES: { value: FiltreStatut; label: string }[] = [
  { value: 'tous', label: 'Tous' },
  { value: 'ouvert', label: 'Ouvert' },
  { value: 'sous_controle', label: 'Sous contrôle' },
  { value: 'rejete', label: 'Rejeté' },
  { value: 'clos', label: 'Clos' },
];

const STATUT_LABEL: Record<StatutRisque, { label: string; bg: string; text: string }> = {
  ouvert: { label: 'Ouvert', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)' },
  sous_controle: { label: 'Sous contrôle', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
  rejete: { label: 'Rejeté', bg: 'var(--color-error-bg)', text: 'var(--color-text-secondary)' },
  clos: { label: 'Clos', bg: 'var(--status-a-faire-bg)', text: 'var(--status-a-faire-text)' },
};

const SOLUTION_STATUT_LABEL: Record<'appliquee' | 'rejetee', { label: string; bg: string; text: string }> = {
  appliquee: { label: '✓ Appliquée', bg: 'var(--status-terminee-bg)', text: 'var(--status-terminee-text)' },
  rejetee: { label: 'Non retenue', bg: 'var(--color-surface-alt)', text: 'var(--color-text-tertiary)' },
};

@Component({
  selector: 'app-project-risks-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState],
  templateUrl: './project-risks-page.html',
  styleUrl: './project-risks-page.css',
})
export class ProjectRisksPage {
  readonly projectId = input.required<string>();

  private readonly risquesService = inject(RisquesProjetService);
  private readonly peopleService = inject(PeopleService);
  private readonly toast = inject(ToastService);

  protected readonly criticiteLabel = CRITICITE_LABEL;
  protected readonly statutLabel = STATUT_LABEL;
  protected readonly solutionStatutLabel = SOLUTION_STATUT_LABEL;
  protected readonly criticiteRisque = criticiteRisque;
  protected readonly formatDateCourteFr = formatDateCourteFr;
  protected readonly filtres = FILTRES;

  protected readonly risks = computed(() => this.risquesService.byProject(this.projectId())());
  protected readonly filtreActif = signal<FiltreStatut>('tous');

  protected readonly compteParFiltre = computed(() => {
    const risks = this.risks();
    const counts: Record<FiltreStatut, number> = { tous: risks.length, ouvert: 0, sous_controle: 0, rejete: 0, clos: 0 };
    for (const r of risks) counts[r.statut]++;
    return counts;
  });

  protected readonly risquesFiltres = computed(() => {
    const filtre = this.filtreActif();
    const risks = this.risks();
    return filtre === 'tous' ? risks : risks.filter((r) => r.statut === filtre);
  });

  protected readonly risqueOuvert = signal<string | null>(null);
  protected readonly solutionsChoisies = signal<Record<string, Set<string>>>({});

  protected readonly detectionEnCours = computed(() => this.risquesService.estEnDetection(this.projectId()));

  estEnRecherche(riskId: string): boolean {
    return this.risquesService.estEnRecherche(riskId);
  }

  estEnApplication(riskId: string): boolean {
    return this.risquesService.estEnApplication(riskId);
  }

  changerFiltre(filtre: FiltreStatut): void {
    this.filtreActif.set(filtre);
  }

  porteur(risk: RisqueProjet) {
    return this.peopleService.byId(risk.porteurId);
  }

  estOuvert(riskId: string): boolean {
    return this.risqueOuvert() === riskId;
  }

  /**
   * Ouvre le panneau de solutions. Pour un risque ouvert sans solution encore proposée, la recherche est lancée
   * d'abord (jusqu'à plusieurs minutes). Pour un risque déjà traité, affiche directement l'historique des
   * recommandations (déjà chargé) en lecture seule.
   */
  async toggleOuverture(risk: RisqueProjet): Promise<void> {
    if (this.estOuvert(risk.id)) {
      this.risqueOuvert.set(null);
      return;
    }
    if (risk.statut === 'ouvert' && risk.solutions.length === 0) {
      try {
        await this.risquesService.suggerer(risk);
      } catch {
        return; // Message d'erreur déjà affiché par l'intercepteur HTTP global.
      }
    }
    this.risqueOuvert.set(risk.id);
  }

  estSolutionChoisie(riskId: string, solutionId: string): boolean {
    return this.solutionsChoisies()[riskId]?.has(solutionId) ?? false;
  }

  solutionsSelectionneesCount(riskId: string): number {
    return this.solutionsChoisies()[riskId]?.size ?? 0;
  }

  toggleSolution(riskId: string, solutionId: string): void {
    this.solutionsChoisies.update((carte) => {
      const courant = new Set(carte[riskId] ?? []);
      if (courant.has(solutionId)) courant.delete(solutionId);
      else courant.add(solutionId);
      return { ...carte, [riskId]: courant };
    });
  }

  async appliquerSolutions(risk: RisqueProjet): Promise<void> {
    const ids = [...(this.solutionsChoisies()[risk.id] ?? [])];
    if (ids.length === 0) return;
    try {
      const traitees = await this.risquesService.appliquer(risk, ids);
      this.toast.show({ type: 'success', message: `${traitees.length} solution(s) appliquée(s) au risque ${risk.code}.` });
      const remarques = traitees.filter((t) => t.remarque).map((t) => `${t.codeTache ?? t.libelle} : ${t.remarque}`);
      if (remarques.length > 0) this.toast.show({ type: 'info', message: remarques.join(' — ') });
      this.solutionsChoisies.update((carte) => ({ ...carte, [risk.id]: new Set() }));
      this.risqueOuvert.set(null);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  /** Lance la détection par l'assistant IA : peut durer plusieurs minutes, le bouton reste désactivé le temps de l'analyse. */
  async detecterRisques(): Promise<void> {
    if (this.detectionEnCours()) return;
    this.toast.show({ type: 'info', message: 'Analyse des risques lancée : elle peut prendre plusieurs minutes.' });
    try {
      const resultat = await this.risquesService.detecter(this.projectId());
      const parties = [`${resultat.nouveaux} nouveau(x) risque(s) détecté(s)`];
      if (resultat.reevalues > 0) parties.push(`${resultat.reevalues} réévalué(s)`);
      if (resultat.clos > 0) parties.push(`${resultat.clos} clos`);
      this.toast.show({ type: 'success', message: `${parties.join(', ')}.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  async rejeterAnalyse(risk: RisqueProjet): Promise<void> {
    try {
      await this.risquesService.rejeter(risk);
      this.risqueOuvert.set(null);
      this.toast.show({ type: 'info', message: `Analyse du risque ${risk.code} rejetée.` });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }
}
