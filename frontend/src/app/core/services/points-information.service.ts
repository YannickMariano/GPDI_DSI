import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';

interface Page<T> {
  content: T[];
}

interface PointInformationResponse {
  id: number;
  titre: string;
  corps: string;
  dateCreation: string;
  idAuteur: number | null;
  nomAuteur: string | null;
}

/** Point d'information publié par le chef de projet à destination des comptes clients partagés sur le projet. */
export interface PointInformation {
  id: string;
  projectId: string;
  titre: string;
  corps: string;
  date: string;
  auteurNom?: string;
}

function versPoint(dto: PointInformationResponse, projectId: string): PointInformation {
  return {
    id: String(dto.id),
    projectId,
    titre: dto.titre,
    corps: dto.corps,
    date: dto.dateCreation,
    auteurNom: dto.nomAuteur ?? undefined,
  };
}

/**
 * Points d'information d'un projet (entité backend `PointInformation`, `/api/projets/{idProjet}/points-information`).
 * Réservé au chef de projet (publication et lecture) et aux comptes clients ayant un partage actif sur ce projet (lecture).
 */
@Injectable({ providedIn: 'root' })
export class PointsInformationService {
  private readonly http = inject(HttpClient);
  private readonly _points = signal<PointInformation[]>([]);

  private readonly projetsCharges = new Set<string>();

  reload(projectId: string): Promise<void> {
    return firstValueFrom(
      this.http.get<Page<PointInformationResponse>>(`${environment.apiUrl}/projets/${projectId}/points-information`, { params: { size: '1000' } }),
    )
      .then((page) => {
        const nouveaux = page.content.map((dto) => versPoint(dto, projectId));
        this._points.update((list) => [...list.filter((p) => p.projectId !== projectId), ...nouveaux]);
        this.projetsCharges.add(projectId);
      })
      .catch(() => undefined);
  }

  ensureLoaded(projectId: string): void {
    if (!this.projetsCharges.has(projectId)) void this.reload(projectId);
  }

  byProject(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._points().filter((p) => p.projectId === projectId).sort((a, b) => b.date.localeCompare(a.date)));
  }

  /** Réservé au chef de projet (responsable) du projet ; recharge la liste avec le nouveau point en tête. */
  async publier(projectId: string, payload: { titre: string; corps: string }): Promise<PointInformation> {
    const dto = await firstValueFrom(
      this.http.post<PointInformationResponse>(`${environment.apiUrl}/projets/${projectId}/points-information`, payload),
    );
    const point = versPoint(dto, projectId);
    this._points.update((list) => [point, ...list]);
    return point;
  }
}
