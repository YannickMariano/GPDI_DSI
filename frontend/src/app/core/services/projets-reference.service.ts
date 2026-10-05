import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { ProjetReference } from '@core/models/projet-reference.model';

interface Page<T> {
  content: T[];
}

interface ProjetResponse {
  id: number;
  titre: string;
  statut: string;
}

/**
 * Liste minimale des projets réels (id/titre/statut), utilisée pour les sélecteurs qui doivent
 * référencer un vrai projet (ex. Partage Client) sans dépendre du module Projets encore mocké.
 */
@Injectable({ providedIn: 'root' })
export class ProjetsReferenceService {
  private readonly http = inject(HttpClient);
  private readonly _projets = signal<ProjetReference[]>([]);
  readonly projets = this._projets.asReadonly();

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<Page<ProjetResponse>>(`${environment.apiUrl}/projets`, { params: { size: '1000' } }))
      .then((page) => this._projets.set(page.content.map((dto) => ({ id: String(dto.id), titre: dto.titre, statut: dto.statut }))))
      .catch(() => undefined);
  }

  byId(id: string): ProjetReference | undefined {
    return this._projets().find((p) => p.id === id);
  }
}
