import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RecommandationsProjetService, RecommandationProjet, TypeRecommandationProjet } from '@core/services/recommandations-projet.service';
import { AssistantIaService } from '@core/services/assistant-ia.service';
import { ProjectsService } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { ToastService } from '@shared/components/toast/toast.service';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';

const TYPE_LABEL: Record<TypeRecommandationProjet, { label: string; bg: string; text: string; action: string }> = {
  planification: { label: 'Planification', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)', action: 'Retirer du sprint' },
  affectation: { label: 'Affectation', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)', action: 'Réaffecter' },
  estimation: { label: 'Estimation', bg: 'var(--color-ai-bg)', text: 'var(--color-ai)', action: "Mettre à jour l'estimation" },
};

const PROMPTS_SUGGERES = ['Qui est en surcharge ?', 'Résumer la semaine', 'Quels sont les risques actifs ?'];

@Component({
  selector: 'app-ai-assistant-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, RelativeTimePipe],
  templateUrl: './ai-assistant-page.html',
  styleUrl: './ai-assistant-page.css',
})
export class AiAssistantPage {
  private readonly recommandationsService = inject(RecommandationsProjetService);
  private readonly assistantIaService = inject(AssistantIaService);
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly typeLabel = TYPE_LABEL;
  protected readonly promptsSuggeres = PROMPTS_SUGGERES;
  protected readonly saisie = signal('');
  protected readonly enTraitement = signal<Set<string>>(new Set());

  /** Projets dont l'utilisateur connecté est le chef de projet : seul le responsable d'un projet a accès à son assistant. */
  protected readonly projetsDisponibles = computed(() => {
    const user = this.authService.currentUser();
    return user ? this.projectsService.projects().filter((p) => p.responsableId === user.id) : [];
  });

  private readonly projetChoisi = signal<string | undefined>(undefined);
  protected readonly projetId = computed(() => {
    const choisi = this.projetChoisi();
    const dispo = this.projetsDisponibles();
    return choisi && dispo.some((p) => p.id === choisi) ? choisi : (dispo[0]?.id ?? '');
  });

  protected readonly projetSelectionne = computed(() => this.projetsDisponibles().find((p) => p.id === this.projetId()));
  protected readonly recommandations = computed(() => (this.projetId() ? this.recommandationsService.byProject(this.projetId())() : []));
  protected readonly analyseEnCours = computed(() => (this.projetId() ? this.recommandationsService.estEnAnalyse(this.projetId()) : false));
  protected readonly messages = computed(() => (this.projetId() ? this.assistantIaService.byProject(this.projetId())() : []));
  protected readonly enCours = computed(() => (this.projetId() ? this.assistantIaService.estEnCours(this.projetId()) : false));

  selectProjet(id: string): void {
    this.projetChoisi.set(id);
  }

  /** Lance l'analyse par l'assistant IA : peut durer plusieurs minutes. */
  async analyser(): Promise<void> {
    const projetId = this.projetId();
    if (!projetId || this.analyseEnCours()) return;
    this.toast.show({ type: 'info', message: 'Analyse du projet lancée : elle peut prendre plusieurs minutes.' });
    try {
      const nombre = await this.recommandationsService.analyser(projetId);
      this.toast.show({ type: 'success', message: nombre > 0 ? `${nombre} nouvelle(s) recommandation(s).` : 'Aucune nouvelle recommandation : le projet suit sa trajectoire.' });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  enTraitementDe(id: string): boolean {
    return this.enTraitement().has(id);
  }

  async appliquer(recommandation: RecommandationProjet): Promise<void> {
    this.marquerEnTraitement(recommandation.id, true);
    try {
      const traitee = await this.recommandationsService.appliquer(recommandation);
      this.toast.show({ type: 'success', message: traitee.resultat ?? 'Recommandation appliquée.' });
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    } finally {
      this.marquerEnTraitement(recommandation.id, false);
    }
  }

  async ignorer(recommandation: RecommandationProjet): Promise<void> {
    this.marquerEnTraitement(recommandation.id, true);
    try {
      await this.recommandationsService.rejeter(recommandation);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    } finally {
      this.marquerEnTraitement(recommandation.id, false);
    }
  }

  private marquerEnTraitement(id: string, actif: boolean): void {
    this.enTraitement.update((set) => {
      const copie = new Set(set);
      if (actif) copie.add(id);
      else copie.delete(id);
      return copie;
    });
  }

  /** Peut durer jusqu'à plusieurs minutes : le fil affiche la question aussitôt, la réponse arrive ensuite. */
  async envoyerPrompt(texte: string): Promise<void> {
    const value = texte.trim();
    const projetId = this.projetId();
    if (!value || !projetId || this.enCours()) return;
    this.saisie.set('');
    try {
      await this.assistantIaService.poserQuestion(projetId, value);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  envoyer(): void {
    void this.envoyerPrompt(this.saisie());
  }
}
