import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, effect, inject, input, signal, viewChild } from '@angular/core';
import { AssistantIaService } from '@core/services/assistant-ia.service';
import { ProjectsService } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { ToastService } from '@shared/components/toast/toast.service';
import { EmptyState } from '@shared/components/empty-state/empty-state';

const PROMPTS_SUGGERES = ['Qui est en surcharge ?', 'Résumer la semaine', 'Quels sont les risques actifs ?'];

/** Message affiché quand le modèle IA est saturé ou indisponible (502/503/504) au lieu de l'erreur technique. */
const MESSAGE_IA_INDISPONIBLE = 'Désolé, je rencontre un problème technique pour le moment. Réessayez dans quelques secondes.';
const STATUTS_IA_INDISPONIBLE = [502, 503, 504];

/** Étapes affichées à tour de rôle pendant l'attente (indicatives : le backend ne renvoie pas de progression). */
const ETAPES_CHAT = ['Lecture de vos projets…', 'Analyse des tâches et des équipes…', 'Rédaction de la réponse…'];
const DUREE_ETAPE_SECONDES = 4;

let prochainIdentifiant = 0;

/**
 * Fil de discussion avec l'assistant IA sur l'ensemble des projets de l'utilisateur. L'état (messages, attente,
 * erreur) vit dans AssistantIaService : l'onglet Assistant IA et la bulle de discussion affichent le même fil.
 */
@Component({
  selector: 'app-assistant-conversation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState],
  host: { '[class.assistant-conversation--compact]': 'compact()' },
  templateUrl: './assistant-conversation.html',
  styleUrl: './assistant-conversation.css',
})
export class AssistantConversation {
  private readonly assistantIaService = inject(AssistantIaService);
  private readonly projectsService = inject(ProjectsService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  /** Variante resserrée (bulle de discussion) : bulles plus larges, police légèrement réduite. */
  readonly compact = input(false);

  protected readonly inputId = `assistant-conversation-input-${prochainIdentifiant++}`;
  protected readonly promptsSuggeres = PROMPTS_SUGGERES;
  protected readonly saisie = signal('');

  private readonly champSaisie = viewChild<ElementRef<HTMLInputElement>>('champSaisie');
  private readonly filMessages = viewChild<ElementRef<HTMLElement>>('filMessages');

  protected readonly aDesProjets = computed(() => {
    const user = this.authService.currentUser();
    return !!user && this.projectsService.projects().some((p) => p.responsableId === user.id);
  });
  protected readonly messages = this.assistantIaService.messagesGlobaux();
  protected readonly enCours = this.assistantIaService.enCoursGlobal;
  protected readonly erreur = this.assistantIaService.erreurGlobale;

  private readonly maintenant = signal(Date.now());
  protected readonly secondes = computed(() => {
    const debut = this.assistantIaService.debutGlobal();
    return debut === null ? 0 : Math.max(0, Math.floor((this.maintenant() - debut) / 1000));
  });
  protected readonly etape = computed(() => ETAPES_CHAT[Math.min(Math.floor(this.secondes() / DUREE_ETAPE_SECONDES), ETAPES_CHAT.length - 1)]);

  constructor() {
    const horloge = setInterval(() => this.maintenant.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(horloge));
    // Garde le dernier message visible à chaque nouveau message ou changement d'état d'attente.
    effect(() => {
      this.messages();
      this.enCours();
      this.erreur();
      const fil = this.filMessages()?.nativeElement;
      if (fil) queueMicrotask(() => (fil.scrollTop = fil.scrollHeight));
    });
  }

  focusSaisie(): void {
    this.champSaisie()?.nativeElement.focus();
  }

  /** Peut durer jusqu'à plusieurs minutes : le fil affiche la question aussitôt, la réponse arrive ensuite. */
  async envoyerPrompt(texte: string): Promise<void> {
    const value = texte.trim();
    if (!value || this.enCours()) return;
    this.saisie.set('');
    try {
      await this.assistantIaService.poserQuestionGlobale(value);
    } catch (erreur) {
      this.traiterErreurIa(erreur);
    }
  }

  envoyer(): void {
    void this.envoyerPrompt(this.saisie());
  }

  actionEnTraitement(idAction: string): boolean {
    return this.assistantIaService.estActionEnTraitement(idAction);
  }

  async confirmerAction(idAction: string): Promise<void> {
    try {
      await this.assistantIaService.confirmerAction(idAction);
    } catch {
      // Message d'erreur (règle métier, droit manquant…) déjà affiché par l'intercepteur HTTP global.
    }
  }

  async annulerAction(idAction: string): Promise<void> {
    try {
      await this.assistantIaService.annulerAction(idAction);
    } catch {
      // Message d'erreur déjà affiché par l'intercepteur HTTP global.
    }
  }

  /**
   * Les appels IA n'affichent pas le toast global : saturation/indisponibilité du modèle (502/503/504) →
   * message convivial dans le fil ; toute autre erreur → toast avec le message du backend, comme ailleurs.
   */
  private traiterErreurIa(erreur: unknown): void {
    if (erreur instanceof HttpErrorResponse && STATUTS_IA_INDISPONIBLE.includes(erreur.status)) {
      this.assistantIaService.signalerErreurGlobale(MESSAGE_IA_INDISPONIBLE);
      return;
    }
    if (erreur instanceof HttpErrorResponse && erreur.status === 401) return;
    const message = erreur instanceof HttpErrorResponse && typeof erreur.error?.message === 'string' ? erreur.error.message : 'Une erreur est survenue. Veuillez réessayer.';
    this.toast.show({ type: 'error', message });
  }
}
