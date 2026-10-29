import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { RecommandationsProjetService, RecommandationProjet, TypeRecommandationProjet } from '@core/services/recommandations-projet.service';
import { ProjectsService } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { ToastService } from '@shared/components/toast/toast.service';
import { EmptyState } from '@shared/components/empty-state/empty-state';
import { RelativeTimePipe } from '@shared/pipes/relative-time.pipe';
import { AssistantConversation } from '../components/assistant-conversation/assistant-conversation';

const TYPE_LABEL: Record<TypeRecommandationProjet, { label: string; bg: string; text: string; action: string }> = {
  planification: { label: 'Planification', bg: 'var(--status-en-cours-bg)', text: 'var(--status-en-cours-text)', action: 'Retirer du sprint' },
  affectation: { label: 'Affectation', bg: 'var(--priority-haute-bg)', text: 'var(--priority-haute-text)', action: 'Réaffecter' },
  estimation: { label: 'Estimation', bg: 'var(--color-ai-bg)', text: 'var(--color-ai)', action: "Mettre à jour l'estimation" },
};

/** Message affiché quand le modèle IA est saturé ou indisponible (502/503/504) au lieu de l'erreur technique. */
const MESSAGE_IA_INDISPONIBLE = 'Désolé, je rencontre un problème technique pour le moment. Réessayez dans quelques secondes.';
const STATUTS_IA_INDISPONIBLE = [502, 503, 504];

/** Étapes affichées à tour de rôle pendant l'attente (indicatives : le backend ne renvoie pas de progression). */
const ETAPES_ANALYSE = ['Lecture du contexte du projet…', 'Analyse de la charge et des délais…', 'Recherche de recommandations…', 'Vérification des propositions…'];
const DUREE_ETAPE_SECONDES = 4;

@Component({
  selector: 'app-ai-assistant-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState, RelativeTimePipe, AssistantConversation],
  templateUrl: './ai-assistant-page.html',
  styleUrl: './ai-assistant-page.css',
})
export class AiAssistantPage {
  private readonly recommandationsService = inject(RecommandationsProjetService);
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly typeLabel = TYPE_LABEL;
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

  // --- Indicateurs d'attente et erreurs de l'IA (analyse des recommandations) ---

  /** Message d'indisponibilité de l'IA, rattaché au projet concerné. */
  private readonly erreurAnalyseParProjet = signal<{ projetId: string; texte: string } | null>(null);
  protected readonly erreurAnalyse = computed(() => (this.erreurAnalyseParProjet()?.projetId === this.projetId() ? this.erreurAnalyseParProjet()?.texte : undefined));

  private readonly maintenant = signal(Date.now());
  private readonly debutAnalyse = signal<number | null>(null);
  protected readonly secondesAnalyse = computed(() => this.secondesDepuis(this.debutAnalyse()));
  protected readonly etapeAnalyse = computed(() => ETAPES_ANALYSE[Math.min(Math.floor(this.secondesAnalyse() / DUREE_ETAPE_SECONDES), ETAPES_ANALYSE.length - 1)]);

  constructor() {
    const horloge = setInterval(() => this.maintenant.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(horloge));
    // Démarre/arrête le compteur d'attente selon l'état « en cours » tenu par le service (il survit à la navigation).
    effect(() => {
      const actif = this.analyseEnCours();
      untracked(() => this.debutAnalyse.update((debut) => (actif ? (debut ?? Date.now()) : null)));
    });
  }

  private secondesDepuis(debut: number | null): number {
    return debut === null ? 0 : Math.max(0, Math.floor((this.maintenant() - debut) / 1000));
  }

  /**
   * Les appels IA n'affichent pas le toast global : saturation/indisponibilité du modèle (502/503/504) →
   * message convivial dans la page ; toute autre erreur → toast avec le message du backend, comme ailleurs.
   */
  private traiterErreurIa(erreur: unknown, projetId: string): void {
    if (erreur instanceof HttpErrorResponse && STATUTS_IA_INDISPONIBLE.includes(erreur.status)) {
      this.erreurAnalyseParProjet.set({ projetId, texte: MESSAGE_IA_INDISPONIBLE });
      return;
    }
    if (erreur instanceof HttpErrorResponse && erreur.status === 401) return;
    const message = erreur instanceof HttpErrorResponse && typeof erreur.error?.message === 'string' ? erreur.error.message : 'Une erreur est survenue. Veuillez réessayer.';
    this.toast.show({ type: 'error', message });
  }

  selectProjet(id: string): void {
    this.projetChoisi.set(id);
  }

  /** Lance l'analyse par l'assistant IA : peut durer plusieurs minutes. */
  async analyser(): Promise<void> {
    const projetId = this.projetId();
    if (!projetId || this.analyseEnCours()) return;
    this.erreurAnalyseParProjet.set(null);
    this.toast.show({ type: 'info', message: 'Analyse du projet lancée : elle peut prendre plusieurs minutes.' });
    try {
      const nombre = await this.recommandationsService.analyser(projetId);
      this.toast.show({ type: 'success', message: nombre > 0 ? `${nombre} nouvelle(s) recommandation(s).` : 'Aucune nouvelle recommandation : le projet suit sa trajectoire.' });
    } catch (erreur) {
      this.traiterErreurIa(erreur, projetId);
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
}
