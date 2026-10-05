import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { BurndownPoint } from '@core/models/burndown.model';

interface BurndownPointDto {
  date: string;
  restant: number | null;
  ideal: number;
}

function formatJour(dateIso: string): string {
  const [, mois, jour] = dateIso.split('-');
  return `${jour}/${mois}`;
}

/** Reconstitue les jours futurs (pas encore d'instantané réel) en prolongeant à plat la dernière valeur réelle connue. */
function versPoints(dtos: BurndownPointDto[]): BurndownPoint[] {
  let dernierConnu: number | undefined;
  return dtos.map((dto) => {
    const futur = dto.restant == null;
    if (!futur) dernierConnu = dto.restant!;
    return { jour: formatJour(dto.date), restant: dernierConnu ?? dto.ideal, ideal: dto.ideal, futur };
  });
}

/** Burndown d'un sprint (calculé côté backend, `/api/projets/{idProjet}/sprints/{idSprint}/burndown`). */
@Injectable({ providedIn: 'root' })
export class BurndownService {
  private readonly http = inject(HttpClient);
  private readonly _points = signal<Record<string, BurndownPoint[]>>({});
  private readonly sprintsCharges = new Set<string>();

  reload(projectId: string, sprintId: string): void {
    firstValueFrom(this.http.get<BurndownPointDto[]>(`${environment.apiUrl}/projets/${projectId}/sprints/${sprintId}/burndown`))
      .then((dtos) => {
        this._points.update((map) => ({ ...map, [sprintId]: versPoints(dtos) }));
        this.sprintsCharges.add(sprintId);
      })
      .catch(() => undefined);
  }

  ensureLoaded(projectId: string, sprintId: string): void {
    if (!this.sprintsCharges.has(sprintId)) this.reload(projectId, sprintId);
  }

  byProjectAndSprint(projectId: string, sprintId: string) {
    this.ensureLoaded(projectId, sprintId);
    return computed(() => this._points()[sprintId] ?? []);
  }
}
