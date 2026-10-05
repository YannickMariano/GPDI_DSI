import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { Role, RoleRecord } from '@core/models/role.model';

interface Page<T> {
  content: T[];
}

/** Rôles applicatifs persistés (entité backend `Role`, `/api/roles`) — les 5 rôles système attendus par le frontend (`core/models/role.model.ts`). */
@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly http = inject(HttpClient);
  private readonly _roles = signal<RoleRecord[]>([]);
  readonly roles = this._roles.asReadonly();

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<Page<RoleRecord>>(`${environment.apiUrl}/roles`, { params: { size: '100' } }))
      .then((page) => this._roles.set(page.content))
      .catch(() => undefined);
  }

  /** Retourne les rôles déjà chargés, ou les charge une première fois si besoin (ex. avant résolution d'un idRole pour une création). */
  async ensureLoaded(): Promise<RoleRecord[]> {
    if (this._roles().length > 0) return this._roles();
    const page = await firstValueFrom(this.http.get<Page<RoleRecord>>(`${environment.apiUrl}/roles`, { params: { size: '100' } }));
    this._roles.set(page.content);
    return page.content;
  }

  byNom(nom: Role): RoleRecord | undefined {
    return this._roles().find((r) => r.nom === nom);
  }

  async modifierDescription(id: number, nom: string, description: string): Promise<RoleRecord> {
    const reponse = await firstValueFrom(this.http.put<RoleRecord>(`${environment.apiUrl}/roles/${id}`, { nom, description }));
    this._roles.update((list) => list.map((r) => (r.id === id ? reponse : r)));
    return reponse;
  }
}
