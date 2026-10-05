import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { HabilitationLigneRequest, NiveauAcces, PermissionRow } from '@core/models/habilitation.model';
import { Role } from '@core/models/role.model';

interface HabilitationResponse {
  module: string;
  fonction: string;
  acces: Record<string, NiveauAcces>;
}

/** Matrice d'habilitations (`/api/habilitations`) : persistée en base et appliquée dynamiquement par le backend. */
@Injectable({ providedIn: 'root' })
export class HabilitationsService {
  private readonly http = inject(HttpClient);
  private readonly _matrice = signal<PermissionRow[]>([]);
  readonly matrice = this._matrice.asReadonly();

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<HabilitationResponse[]>(`${environment.apiUrl}/habilitations`))
      .then((lignes) => this._matrice.set(lignes.map(versLigne)))
      .catch(() => undefined);
  }

  async mettreAJour(lignes: HabilitationLigneRequest[]): Promise<PermissionRow[]> {
    const reponse = await firstValueFrom(this.http.put<HabilitationResponse[]>(`${environment.apiUrl}/habilitations`, lignes));
    const matrice = reponse.map(versLigne);
    this._matrice.set(matrice);
    return matrice;
  }
}

function versLigne(ligne: HabilitationResponse): PermissionRow {
  return { fonction: ligne.fonction, module: ligne.module, acces: ligne.acces as Record<Role, NiveauAcces> };
}
