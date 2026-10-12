import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { SANS_TOAST_ERREUR } from '@core/interceptors/error.interceptor';
import { TasksService } from './tasks.service';

interface RecommandationProjetResponse {
  id: number;
  type: string;
  libelle: string;
  justification: string | null;
  preuve: string | null;
  statut: string;
  resultat: string | null;
  dateCreation: string;
}

export type TypeRecommandationProjet = 'planification' | 'affectation' | 'estimation';
export type StatutRecommandationProjet = 'proposee' | 'appliquee' | 'rejetee';

const TYPE_VERS_FRONTEND: Record<string, TypeRecommandationProjet> = {
  RETIRER_DU_SPRINT: 'planification',
  REASSIGNER_TACHE: 'affectation',
  AJUSTER_ESTIMATION: 'estimation',
};

/** Recommandation proactive de pilotage (entité backend générique `IaRecommandation`, `/api/projets/{idProjet}/recommandations`). */
export interface RecommandationProjet {
  id: string;
  projectId: string;
  type: TypeRecommandationProjet;
  libelle: string;
  justification: string;
  preuve?: string;
  statut: StatutRecommandationProjet;
  resultat?: string;
  quand: string;
}

function versRecommandation(dto: RecommandationProjetResponse, projectId: string): RecommandationProjet {
  return {
    id: String(dto.id),
    projectId,
    type: TYPE_VERS_FRONTEND[dto.type] ?? 'planification',
    libelle: dto.libelle,
    justification: dto.justification ?? '',
    preuve: dto.preuve ?? undefined,
    statut: dto.statut.toLowerCase() as StatutRecommandationProjet,
    resultat: dto.resultat ?? undefined,
    quand: dto.dateCreation,
  };
}

/**
 * Recommandations proactives de pilotage (réaffectation, désengorgement de sprint, réestimation),
 * générées à la demande par l'assistant IA. Réservé au chef de projet (responsable) du projet.
 * L'analyse peut durer jusqu'à plusieurs minutes : son état « en cours » vit ici, par projet.
 */
@Injectable({ providedIn: 'root' })
export class RecommandationsProjetService {
  private readonly http = inject(HttpClient);
  private readonly tasksService = inject(TasksService);

  private readonly _recommandations = signal<RecommandationProjet[]>([]);
  private readonly _enAnalyse = signal<ReadonlySet<string>>(new Set());

  /** Toutes les recommandations en attente déjà chargées (tous projets confondus) — utilisé pour le badge de navigation. */
  readonly recommandations = this._recommandations.asReadonly();

  private readonly projetsCharges = new Set<string>();

  /** `silencieux` : pas de message d'erreur (chargement d'arrière-plan, ex. Directeur DSI consultant un projet dont il n'est pas le chef). */
  reload(projectId: string, silencieux = false): Promise<void> {
    const context = silencieux ? new HttpContext().set(SANS_TOAST_ERREUR, true) : undefined;
    return firstValueFrom(this.http.get<RecommandationProjetResponse[]>(`${environment.apiUrl}/projets/${projectId}/recommandations`, { context }))
      .then((dtos) => {
        const nouvelles = dtos.map((dto) => versRecommandation(dto, projectId));
        this._recommandations.update((list) => [...list.filter((r) => r.projectId !== projectId), ...nouvelles]);
        this.projetsCharges.add(projectId);
      })
      .catch(() => undefined);
  }

  ensureLoaded(projectId: string): void {
    if (!this.projetsCharges.has(projectId)) void this.reload(projectId, true);
  }

  /** Recommandations en attente (proposées) du projet. */
  byProject(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._recommandations().filter((r) => r.projectId === projectId));
  }

  estEnAnalyse(projectId: string): boolean {
    return this._enAnalyse().has(projectId);
  }

  /** Lance l'analyse du projet par l'assistant IA (jusqu'à plusieurs minutes), puis recharge la liste. */
  async analyser(projectId: string): Promise<number> {
    this.marquer(projectId, true);
    try {
      const dtos = await firstValueFrom(
        // Erreur gérée par la page (message adapté en cas de saturation de l'IA) : pas de toast global.
        this.http.post<RecommandationProjetResponse[]>(`${environment.apiUrl}/projets/${projectId}/recommandations/analyser`, {}, {
          context: new HttpContext().set(SANS_TOAST_ERREUR, true),
        }),
      );
      const nouvelles = dtos.map((dto) => versRecommandation(dto, projectId));
      this._recommandations.update((list) => [...list, ...nouvelles]);
      return nouvelles.length;
    } finally {
      this.marquer(projectId, false);
    }
  }

  async appliquer(recommandation: RecommandationProjet): Promise<RecommandationProjet> {
    const dto = await firstValueFrom(
      this.http.post<RecommandationProjetResponse>(
        `${environment.apiUrl}/projets/${recommandation.projectId}/recommandations/${recommandation.id}/appliquer`,
        {},
      ),
    );
    // La recommandation a modifié une tâche (réaffectation, retrait du sprint ou réestimation) :
    // le cache des tâches du projet est périmé.
    this.tasksService.reload(recommandation.projectId);
    return this.remplacer(dto, recommandation.projectId);
  }

  async rejeter(recommandation: RecommandationProjet): Promise<RecommandationProjet> {
    const dto = await firstValueFrom(
      this.http.post<RecommandationProjetResponse>(
        `${environment.apiUrl}/projets/${recommandation.projectId}/recommandations/${recommandation.id}/rejeter`,
        {},
      ),
    );
    return this.remplacer(dto, recommandation.projectId);
  }

  private remplacer(dto: RecommandationProjetResponse, projectId: string): RecommandationProjet {
    const recommandation = versRecommandation(dto, projectId);
    // Traitée : elle sort de la liste des « en attente » (l'API ne renvoie que celles-ci sur reload).
    this._recommandations.update((list) => list.filter((r) => r.id !== recommandation.id));
    return recommandation;
  }

  private marquer(projectId: string, actif: boolean): void {
    this._enAnalyse.update((courant) => {
      const copie = new Set(courant);
      if (actif) copie.add(projectId);
      else copie.delete(projectId);
      return copie;
    });
  }
}
