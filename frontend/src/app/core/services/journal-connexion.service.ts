import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';

export type ResultatConnexion = 'succes' | 'echec';

export interface JournalConnexionEntry {
  id: string;
  horodatage: string;
  matricule: string;
  nomAffiche: string;
  evenement: string;
  ip: string;
  resultat: ResultatConnexion;
}

interface JournalConnexionResponse {
  horodatage: string;
  matricule: string;
  nomAffiche: string;
  evenement: string;
  ip: string;
  resultat: ResultatConnexion;
}

/**
 * Historique des connexions — reconstitué à partir des événements Keycloak (LOGIN, LOGIN_ERROR,
 * LOGOUT), voir `/api/journal-connexions`. Nécessite que la journalisation des événements soit
 * activée sur le realm Keycloak ; sinon la liste reste simplement vide.
 */
@Injectable({ providedIn: 'root' })
export class JournalConnexionService {
  private readonly http = inject(HttpClient);
  private readonly _evenements = signal<JournalConnexionEntry[]>([]);
  readonly evenements = this._evenements.asReadonly();

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<JournalConnexionResponse[]>(`${environment.apiUrl}/journal-connexions`, { params: { max: '100' } }))
      .then((liste) => this._evenements.set(liste.map((dto, index) => ({ id: `${dto.horodatage}-${index}`, ...dto }))))
      .catch(() => undefined);
  }
}
