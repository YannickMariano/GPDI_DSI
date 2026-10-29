import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { SANS_TOAST_ERREUR } from '@core/interceptors/error.interceptor';
import { ChatMessage } from '@core/models/ai-recommendation.model';

interface MessageAssistantResponse {
  id: number;
  question: string;
  reponse: string;
  fondement: string | null;
  dateCreation: string;
}

interface MessageProjet {
  projectId: string;
  message: ChatMessage;
}

function versMessages(dto: MessageAssistantResponse, projectId: string): MessageProjet[] {
  return [
    { projectId, message: { id: `${dto.id}-q`, isUser: true, texte: dto.question, quand: dto.dateCreation } },
    { projectId, message: { id: `${dto.id}-r`, isUser: false, texte: dto.reponse, source: dto.fondement ?? undefined, quand: dto.dateCreation } },
  ];
}

/**
 * Assistant IA en langage libre sur le contexte d'un projet (entité backend générique `IaAnalyse`,
 * `/api/projets/{idProjet}/assistant/messages`). Réservé au chef de projet (responsable) du projet.
 * L'appel au modèle peut durer jusqu'à plusieurs minutes : l'état « en cours » vit ici, par projet.
 */
@Injectable({ providedIn: 'root' })
export class AssistantIaService {
  private readonly http = inject(HttpClient);

  private readonly _messages = signal<MessageProjet[]>([]);
  private readonly _enCours = signal<ReadonlySet<string>>(new Set());

  private readonly projetsCharges = new Set<string>();

  // --- Discussion globale (tous les projets de l'utilisateur, `/api/assistant/messages`) ---
  // Partagée entre l'onglet Assistant IA et la bulle de discussion : un seul fil, un seul état « en cours ».
  private readonly _messagesGlobaux = signal<ChatMessage[]>([]);
  private readonly _debutGlobal = signal<number | null>(null);
  private readonly _erreurGlobale = signal<string | null>(null);
  private globalCharge = false;

  readonly enCoursGlobal = computed(() => this._debutGlobal() !== null);
  /** Horodatage du début de l'attente en cours (compteur « x s » identique dans l'onglet et la bulle). */
  readonly debutGlobal = this._debutGlobal.asReadonly();
  readonly erreurGlobale = this._erreurGlobale.asReadonly();

  messagesGlobaux() {
    if (!this.globalCharge) {
      this.globalCharge = true;
      void firstValueFrom(this.http.get<MessageAssistantResponse[]>(`${environment.apiUrl}/assistant/messages`))
        .then((dtos) => this._messagesGlobaux.update((locaux) => [...dtos.flatMap((dto) => versMessages(dto, '').map((m) => m.message)), ...locaux]))
        .catch(() => {
          this.globalCharge = false;
        });
    }
    return this._messagesGlobaux.asReadonly();
  }

  signalerErreurGlobale(texte: string | null): void {
    this._erreurGlobale.set(texte);
  }

  /** Pose une question sur l'ensemble des projets : peut durer plusieurs minutes, le fil affiche la question aussitôt. */
  async poserQuestionGlobale(question: string): Promise<void> {
    const quandLocal = new Date().toISOString();
    this._messagesGlobaux.update((list) => [...list, { id: `local-${quandLocal}`, isUser: true, texte: question, quand: quandLocal }]);
    this._erreurGlobale.set(null);
    this._debutGlobal.set(Date.now());
    try {
      const dto = await firstValueFrom(
        // Erreur gérée par le composant de discussion (message adapté en cas de saturation de l'IA) : pas de toast global.
        this.http.post<MessageAssistantResponse>(`${environment.apiUrl}/assistant/messages`, { question }, {
          context: new HttpContext().set(SANS_TOAST_ERREUR, true),
        }),
      );
      this._messagesGlobaux.update((list) => [
        ...list,
        { id: `${dto.id}-r`, isUser: false, texte: dto.reponse, source: dto.fondement ?? undefined, quand: dto.dateCreation },
      ]);
    } finally {
      this._debutGlobal.set(null);
    }
  }

  reload(projectId: string): Promise<void> {
    return firstValueFrom(this.http.get<MessageAssistantResponse[]>(`${environment.apiUrl}/projets/${projectId}/assistant/messages`))
      .then((dtos) => {
        const nouveaux = dtos.flatMap((dto) => versMessages(dto, projectId));
        this._messages.update((list) => [...list.filter((m) => m.projectId !== projectId), ...nouveaux]);
        this.projetsCharges.add(projectId);
      })
      .catch(() => undefined);
  }

  ensureLoaded(projectId: string): void {
    if (!this.projetsCharges.has(projectId)) void this.reload(projectId);
  }

  byProject(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._messages().filter((m) => m.projectId === projectId).map((m) => m.message));
  }

  estEnCours(projectId: string): boolean {
    return this._enCours().has(projectId);
  }

  /** Pose une question à l'assistant sur le projet : peut durer plusieurs minutes, le fil affiche la question aussitôt. */
  async poserQuestion(projectId: string, question: string): Promise<void> {
    const quandLocal = new Date().toISOString();
    this._messages.update((list) => [
      ...list,
      { projectId, message: { id: `local-${quandLocal}`, isUser: true, texte: question, quand: quandLocal } },
    ]);
    this._enCours.update((courant) => new Set(courant).add(projectId));
    try {
      const dto = await firstValueFrom(
        // Erreur gérée par la page (message adapté en cas de saturation de l'IA) : pas de toast global.
        this.http.post<MessageAssistantResponse>(`${environment.apiUrl}/projets/${projectId}/assistant/messages`, { question }, {
          context: new HttpContext().set(SANS_TOAST_ERREUR, true),
        }),
      );
      this._messages.update((list) => [
        ...list,
        { projectId, message: { id: `${dto.id}-r`, isUser: false, texte: dto.reponse, source: dto.fondement ?? undefined, quand: dto.dateCreation } },
      ]);
    } finally {
      this._enCours.update((courant) => {
        const copie = new Set(courant);
        copie.delete(projectId);
        return copie;
      });
    }
  }
}
