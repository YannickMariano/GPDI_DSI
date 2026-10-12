import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { ReportDocument } from '@core/models/report.model';

interface RapportDiffuseResponse {
  id: number;
  idProjet: number;
  nomProjet: string | null;
  modele: string;
  nom: string;
  titre: string | null;
  contenu: ReportDocument | null;
  idAuteur: number | null;
  nomAuteur: string | null;
  dateDiffusion: string;
}

/** Rapport diffusé par un chef de projet : photo figée du document au moment de la diffusion (lecture seule). */
export interface RapportDiffuse {
  id: string;
  projectId: string;
  nomProjet: string;
  modele: string;
  nom: string;
  titre: string;
  document: ReportDocument | null;
  auteur: string;
  dateDiffusion: string;
}

function versRapport(dto: RapportDiffuseResponse): RapportDiffuse {
  return {
    id: String(dto.id),
    projectId: String(dto.idProjet),
    nomProjet: dto.nomProjet ?? '',
    modele: dto.modele,
    nom: dto.nom,
    titre: dto.titre ?? dto.nom,
    document: dto.contenu,
    auteur: dto.nomAuteur ?? '—',
    dateDiffusion: dto.dateDiffusion,
  };
}

/**
 * Rapports diffusés (`/api/rapports-diffuses`) : le chef de projet diffuse un rapport de son projet ; les
 * membres concernés du projet et le Directeur DSI (tous les projets) les consultent en lecture seule.
 */
@Injectable({ providedIn: 'root' })
export class RapportsDiffusesService {
  private readonly http = inject(HttpClient);

  private readonly _rapports = signal<RapportDiffuse[]>([]);
  readonly rapports = this._rapports.asReadonly();
  private readonly _charge = signal(false);
  readonly charge = this._charge.asReadonly();

  reload(): Promise<void> {
    return firstValueFrom(this.http.get<RapportDiffuseResponse[]>(`${environment.apiUrl}/rapports-diffuses`))
      .then((dtos) => this._rapports.set(dtos.map(versRapport)))
      .catch(() => undefined)
      .finally(() => this._charge.set(true));
  }

  async diffuser(projectId: string, modele: string, document: ReportDocument): Promise<RapportDiffuse> {
    const dto = await firstValueFrom(
      this.http.post<RapportDiffuseResponse>(`${environment.apiUrl}/rapports-diffuses`, {
        idProjet: Number(projectId),
        modele,
        nom: document.nom,
        titre: document.titre,
        contenu: document,
      }),
    );
    const rapport = versRapport(dto);
    this._rapports.update((list) => [rapport, ...list]);
    return rapport;
  }
}
