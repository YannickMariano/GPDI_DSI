import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { Sprint, StatutSprint } from '@core/models/sprint.model';

interface Page<T> {
  content: T[];
}

interface SprintResponse {
  id: number;
  nom: string;
  dateDebut: string;
  dateFin: string;
  objectif: string;
  capaciteHeures: number;
  statut: 'PLANIFIE' | 'EN_COURS' | 'CLOTURER';
  idProjet: number;
}

export interface SprintPayload {
  nom: string;
  dateDebut: string;
  dateFin: string;
  objectif: string;
  capaciteHeures: number;
}

const STATUT_VERS_FRONTEND: Record<SprintResponse['statut'], StatutSprint> = {
  PLANIFIE: 'planifie',
  EN_COURS: 'en_cours',
  CLOTURER: 'cloture',
};

function versSprint(dto: SprintResponse): Sprint {
  return {
    id: String(dto.id),
    projectId: String(dto.idProjet),
    nom: dto.nom,
    statut: STATUT_VERS_FRONTEND[dto.statut],
    dateDebut: dto.dateDebut,
    dateFin: dto.dateFin,
    objectif: dto.objectif ?? '',
    capaciteHeures: dto.capaciteHeures,
  };
}

/** Sprints d'un projet (entité backend `Sprint`, `/api/projets/{idProjet}/sprints`). */
@Injectable({ providedIn: 'root' })
export class SprintsService {
  private readonly http = inject(HttpClient);
  private readonly _sprints = signal<Sprint[]>([]);
  readonly sprints = this._sprints.asReadonly();

  private readonly projetsCharges = new Set<string>();

  /** Charge (ou recharge) les sprints d'un projet. Idempotent : peut être rappelé sans effet de duplication. */
  reload(projectId: string): void {
    firstValueFrom(this.http.get<Page<SprintResponse>>(`${environment.apiUrl}/projets/${projectId}/sprints`, { params: { size: '1000' } }))
      .then((page) => {
        const nouveaux = page.content.map(versSprint);
        this._sprints.update((list) => [...list.filter((s) => s.projectId !== projectId), ...nouveaux]);
        this.projetsCharges.add(projectId);
      })
      .catch(() => undefined);
  }

  /** Charge les sprints du projet s'ils ne l'ont pas encore été. */
  ensureLoaded(projectId: string): void {
    if (!this.projetsCharges.has(projectId)) this.reload(projectId);
  }

  byProject(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._sprints().filter((s) => s.projectId === projectId));
  }

  current(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._sprints().find((s) => s.projectId === projectId && s.statut === 'en_cours'));
  }

  byId(id: string | undefined) {
    return computed(() => this._sprints().find((s) => s.id === id));
  }

  async create(payload: SprintPayload & { projectId: string }): Promise<Sprint> {
    const dto = await firstValueFrom(
      this.http.post<SprintResponse>(`${environment.apiUrl}/projets/${payload.projectId}/sprints`, {
        nom: payload.nom,
        dateDebut: payload.dateDebut,
        dateFin: payload.dateFin,
        objectif: payload.objectif,
        capaciteHeures: payload.capaciteHeures,
      }),
    );
    const sprint = versSprint(dto);
    this._sprints.update((list) => [...list, sprint]);
    return sprint;
  }

  async update(id: string, payload: SprintPayload): Promise<void> {
    const projectId = this.projectIdDe(id);
    const dto = await firstValueFrom(
      this.http.put<SprintResponse>(`${environment.apiUrl}/projets/${projectId}/sprints/${id}`, payload),
    );
    this._sprints.update((list) => list.map((s) => (s.id === id ? versSprint(dto) : s)));
  }

  async demarrer(id: string): Promise<void> {
    const projectId = this.projectIdDe(id);
    const dto = await firstValueFrom(
      this.http.post<SprintResponse>(`${environment.apiUrl}/projets/${projectId}/sprints/${id}/commencer`, {}),
    );
    this._sprints.update((list) => list.map((s) => (s.id === id ? versSprint(dto) : s)));
  }

  async cloturer(id: string): Promise<void> {
    const projectId = this.projectIdDe(id);
    const dto = await firstValueFrom(
      this.http.post<SprintResponse>(`${environment.apiUrl}/projets/${projectId}/sprints/${id}/terminer`, {}),
    );
    this._sprints.update((list) => list.map((s) => (s.id === id ? versSprint(dto) : s)));
  }

  private projectIdDe(sprintId: string): string {
    const sprint = this._sprints().find((s) => s.id === sprintId);
    if (!sprint) throw new Error(`Sprint ${sprintId} introuvable localement.`);
    return sprint.projectId;
  }
}
