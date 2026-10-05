import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { Livrable, Milestone, StatutJalon, StatutLivrable } from '@core/models/milestone.model';

interface Page<T> {
  content: T[];
}

interface JalonResponse {
  id: number;
  titre: string;
  description: string;
  dateEcheance: string;
  statut: 'A_VENIR' | 'EN_RETARD' | 'ATTEINT' | 'ANNULER';
  dateValidation: string | null;
  idProjet: number;
}

interface LivrableResponse {
  id: number;
  description: string;
  type: string;
  statut: 'ATTENDU' | 'LIVRE' | 'REJETE';
  dateLivraison: string | null;
  dateValidation: string | null;
  dateRejet: string | null;
  motifRejet: string | null;
  idJalon: number;
}

const STATUT_JALON_VERS_FRONTEND: Record<JalonResponse['statut'], StatutJalon> = {
  A_VENIR: 'planifie',
  EN_RETARD: 'a_risque',
  ATTEINT: 'atteint',
  ANNULER: 'annule',
};

const STATUT_LIVRABLE_VERS_FRONTEND: Record<LivrableResponse['statut'], StatutLivrable> = {
  ATTENDU: 'a_livrer',
  LIVRE: 'livre',
  REJETE: 'rejete',
};

/** La date "prevue" cote frontend est `dateLivraison` cote backend ; la date "reelle" (a la livraison)
 *  est `dateValidation` cote backend, appelee `dateLivraison` cote frontend. Meme concept, noms differents. */
function versLivrable(dto: LivrableResponse): Livrable {
  return {
    id: String(dto.id),
    description: dto.description,
    type: dto.type,
    dateLivraisonPrevue: dto.dateLivraison ?? '',
    statut: STATUT_LIVRABLE_VERS_FRONTEND[dto.statut],
    dateLivraison: dto.dateValidation ?? undefined,
    dateRejet: dto.dateRejet ?? undefined,
    motifRejet: dto.motifRejet ?? undefined,
  };
}

function versMilestone(dto: JalonResponse, livrables: Livrable[]): Milestone {
  return {
    id: String(dto.id),
    projectId: String(dto.idProjet),
    nom: dto.titre,
    date: dto.dateEcheance,
    statut: STATUT_JALON_VERS_FRONTEND[dto.statut],
    description: dto.description ?? '',
    livrables,
    alerte: dto.statut === 'EN_RETARD' ? `Ce jalon a dépassé son échéance du ${dto.dateEcheance}.` : undefined,
  };
}

/** Jalons d'un projet et leurs livrables (entités backend `Jalon`/`Livrable`). */
@Injectable({ providedIn: 'root' })
export class MilestonesService {
  private readonly http = inject(HttpClient);
  private readonly _milestones = signal<Milestone[]>([]);
  readonly milestones = this._milestones.asReadonly();

  private readonly projetsCharges = new Set<string>();

  reload(projectId: string): void {
    this.chargerProjet(projectId).catch(() => undefined);
  }

  private async chargerProjet(projectId: string): Promise<void> {
    const page = await firstValueFrom(
      this.http.get<Page<JalonResponse>>(`${environment.apiUrl}/projets/${projectId}/jalons`, { params: { size: '1000' } }),
    );
    const milestones = await Promise.all(page.content.map((dto) => this.chargerAvecLivrables(dto)));
    this._milestones.update((list) => [...list.filter((m) => m.projectId !== projectId), ...milestones]);
    this.projetsCharges.add(projectId);
  }

  private async chargerAvecLivrables(dto: JalonResponse): Promise<Milestone> {
    const livrablesPage = await firstValueFrom(
      this.http.get<Page<LivrableResponse>>(`${environment.apiUrl}/jalons/${dto.id}/livrables`, { params: { size: '1000' } }),
    ).catch(() => ({ content: [] as LivrableResponse[] }));
    return versMilestone(dto, livrablesPage.content.map(versLivrable));
  }

  ensureLoaded(projectId: string): void {
    if (!this.projetsCharges.has(projectId)) this.reload(projectId);
  }

  byProject(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._milestones().filter((m) => m.projectId === projectId));
  }

  async create(payload: { projectId: string; nom: string; date: string; description: string }): Promise<Milestone> {
    const dto = await firstValueFrom(
      this.http.post<JalonResponse>(`${environment.apiUrl}/projets/${payload.projectId}/jalons`, {
        titre: payload.nom,
        description: payload.description,
        dateEcheance: payload.date,
      }),
    );
    const milestone = versMilestone(dto, []);
    this._milestones.update((list) => [...list, milestone]);
    return milestone;
  }

  async update(id: string, payload: { nom: string; date: string; description: string }): Promise<void> {
    const projectId = this.projectIdDe(id);
    const dto = await firstValueFrom(
      this.http.put<JalonResponse>(`${environment.apiUrl}/projets/${projectId}/jalons/${id}`, {
        titre: payload.nom,
        description: payload.description,
        dateEcheance: payload.date,
      }),
    );
    this._milestones.update((list) => list.map((m) => (m.id === id ? versMilestone(dto, m.livrables) : m)));
  }

  async terminer(id: string): Promise<void> {
    const projectId = this.projectIdDe(id);
    const dto = await firstValueFrom(this.http.post<JalonResponse>(`${environment.apiUrl}/projets/${projectId}/jalons/${id}/terminer`, {}));
    this._milestones.update((list) => list.map((m) => (m.id === id ? versMilestone(dto, m.livrables) : m)));
  }

  async annuler(id: string): Promise<void> {
    const projectId = this.projectIdDe(id);
    const dto = await firstValueFrom(this.http.post<JalonResponse>(`${environment.apiUrl}/projets/${projectId}/jalons/${id}/annuler`, {}));
    this._milestones.update((list) => list.map((m) => (m.id === id ? versMilestone(dto, m.livrables) : m)));
  }

  async addLivrable(milestoneId: string, payload: { description: string; type: string; dateLivraisonPrevue: string }): Promise<void> {
    const dto = await firstValueFrom(
      this.http.post<LivrableResponse>(`${environment.apiUrl}/jalons/${milestoneId}/livrables`, {
        description: payload.description,
        type: payload.type,
        dateLivraison: payload.dateLivraisonPrevue,
      }),
    );
    const livrable = versLivrable(dto);
    this._milestones.update((list) => list.map((m) => (m.id === milestoneId ? { ...m, livrables: [...m.livrables, livrable] } : m)));
  }

  async livrerLivrable(milestoneId: string, livrableId: string): Promise<void> {
    const dto = await firstValueFrom(this.http.post<LivrableResponse>(`${environment.apiUrl}/jalons/${milestoneId}/livrables/${livrableId}/livrer`, {}));
    this.remplacerLivrable(milestoneId, dto);
  }

  async rejeterLivrable(milestoneId: string, livrableId: string, motif: string): Promise<void> {
    const dto = await firstValueFrom(
      this.http.post<LivrableResponse>(`${environment.apiUrl}/jalons/${milestoneId}/livrables/${livrableId}/rejeter`, { motifRejet: motif }),
    );
    this.remplacerLivrable(milestoneId, dto);
  }

  private remplacerLivrable(milestoneId: string, dto: LivrableResponse): void {
    const livrable = versLivrable(dto);
    this._milestones.update((list) =>
      list.map((m) => (m.id !== milestoneId ? m : { ...m, livrables: m.livrables.map((l) => (l.id === livrable.id ? livrable : l)) })),
    );
  }

  private projectIdDe(milestoneId: string): string {
    const milestone = this._milestones().find((m) => m.id === milestoneId);
    if (!milestone) throw new Error(`Jalon ${milestoneId} introuvable localement.`);
    return milestone.projectId;
  }
}
