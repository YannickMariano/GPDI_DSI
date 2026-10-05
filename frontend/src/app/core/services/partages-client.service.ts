import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';

interface Page<T> {
  content: T[];
}

type StatutPartageApi = 'ACTIF' | 'REVOQUE' | 'EXPIRE';

interface PartageClientResponse {
  id: number;
  idUtilisateur: number;
  nomClient: string;
  idProjet: number;
  titreProjet: string;
  statut: StatutPartageApi;
  dateCreation: string;
  dateEcheance: string;
}

export type EtatPartage = 'actif' | 'revoque' | 'expire';

/** Partage de l'avancement d'un projet avec un compte Direction métier (entité `PartageClient`, `/api/partages-clients`). */
export interface ClientShareRecord {
  id: string;
  clientId: string;
  nomClient: string;
  projectId: string;
  titreProjet: string;
  etat: EtatPartage;
  depuis: string;
  echeance: string;
}

function versEtat(statut: StatutPartageApi): EtatPartage {
  return statut === 'ACTIF' ? 'actif' : statut === 'EXPIRE' ? 'expire' : 'revoque';
}

function versShare(dto: PartageClientResponse): ClientShareRecord {
  return {
    id: String(dto.id),
    clientId: String(dto.idUtilisateur),
    nomClient: dto.nomClient,
    projectId: String(dto.idProjet),
    titreProjet: dto.titreProjet,
    etat: versEtat(dto.statut),
    depuis: dto.dateCreation.slice(0, 10),
    echeance: dto.dateEcheance,
  };
}

@Injectable({ providedIn: 'root' })
export class PartagesClientService {
  private readonly http = inject(HttpClient);
  private readonly _shares = signal<ClientShareRecord[]>([]);
  readonly shares = this._shares.asReadonly();

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<Page<PartageClientResponse>>(`${environment.apiUrl}/partages-clients`, { params: { size: '1000' } }))
      .then((page) => this._shares.set(page.content.map(versShare)))
      .catch(() => undefined);
  }

  async create(payload: { clientId: string; projectIds: string[]; echeance: string }): Promise<ClientShareRecord[]> {
    const dtos = await firstValueFrom(
      this.http.post<PartageClientResponse[]>(`${environment.apiUrl}/partages-clients`, {
        idUtilisateur: Number(payload.clientId),
        idsProjets: payload.projectIds.map(Number),
        dateEcheance: payload.echeance,
      }),
    );
    const nouveaux = dtos.map(versShare);
    this._shares.update((list) => [...list, ...nouveaux]);
    return nouveaux;
  }

  async revoke(id: string): Promise<void> {
    const dto = await firstValueFrom(this.http.post<PartageClientResponse>(`${environment.apiUrl}/partages-clients/${id}/revoquer`, {}));
    const share = versShare(dto);
    this._shares.update((list) => list.map((s) => (s.id === id ? share : s)));
  }

  /** Projets partagés actifs pour un compte Direction métier donné — consommé par l'Espace client. */
  activeSharesForClient(clientId: string) {
    return computed(() => this._shares().filter((s) => s.clientId === clientId && s.etat === 'actif'));
  }
}
