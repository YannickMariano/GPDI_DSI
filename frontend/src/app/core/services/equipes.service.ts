import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';

interface Page<T> {
  content: T[];
}

interface MembreEquipeResponse {
  id: number;
  idUtilisateur: number;
  matricule: string;
  nom: string;
  prenom: string;
  actif: boolean;
}

interface EquipeResponse {
  id: number;
  nom: string;
  description: string | null;
  active: boolean;
  idChefEquipe: number;
  nomChefEquipe: string;
  membres: MembreEquipeResponse[];
}

/** Équipe organisationnelle (nom, chef, membres) — même forme que l'ancien mock (`Team`) pour ne pas impacter les pages consommatrices. */
export interface Team {
  id: string;
  nom: string;
  description?: string;
  chefId: string;
  membreIds: string[];
}

/** Équipes DSI (entité backend `Equipe`/`MembreEquipe`, `/api/equipes`). */
@Injectable({ providedIn: 'root' })
export class EquipesService {
  private readonly http = inject(HttpClient);
  private readonly _teams = signal<Team[]>([]);
  readonly teams = this._teams.asReadonly();

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<Page<EquipeResponse>>(`${environment.apiUrl}/equipes`, { params: { size: '1000' } }))
      .then((page) => this._teams.set(page.content.map((dto) => this.versTeam(dto))))
      .catch(() => undefined);
  }

  async createTeam(payload: { nom: string; chefId: string; description: string; membreIds: string[] }): Promise<Team> {
    const dto = await firstValueFrom(this.http.post<EquipeResponse>(`${environment.apiUrl}/equipes`, this.versRequest(payload)));
    const team = this.versTeam(dto);
    this._teams.update((list) => [...list, team]);
    return team;
  }

  async updateTeam(id: string, payload: { nom: string; chefId: string; description: string; membreIds: string[] }): Promise<void> {
    const dto = await firstValueFrom(this.http.put<EquipeResponse>(`${environment.apiUrl}/equipes/${id}`, this.versRequest(payload)));
    const team = this.versTeam(dto);
    this._teams.update((list) => list.map((t) => (t.id === id ? team : t)));
  }

  async deleteTeam(id: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${environment.apiUrl}/equipes/${id}`));
    this._teams.update((list) => list.filter((t) => t.id !== id));
  }

  private versRequest(payload: { nom: string; chefId: string; description: string; membreIds: string[] }) {
    // Le chef fait toujours partie du roster envoyé au backend (idsMembres ne doit jamais être vide).
    const idsMembres = [...new Set([payload.chefId, ...payload.membreIds])].map(Number);
    return { nom: payload.nom, description: payload.description || null, idChefEquipe: Number(payload.chefId), idsMembres };
  }

  private versTeam(dto: EquipeResponse): Team {
    return {
      id: String(dto.id),
      nom: dto.nom,
      description: dto.description ?? undefined,
      chefId: String(dto.idChefEquipe),
      membreIds: dto.membres.filter((m) => m.idUtilisateur !== dto.idChefEquipe).map((m) => String(m.idUtilisateur)),
    };
  }
}
