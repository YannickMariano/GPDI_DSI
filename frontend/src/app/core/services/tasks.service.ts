import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { ActiviteTache, DependanceTache, PieceJointeActivite, Priorite, SousTache, StatutTache, Task, TypeLienDependance } from '@core/models/task.model';

interface Page<T> {
  content: T[];
}

interface UtilisateurAssigneDto {
  idUtilisateur: number;
}

interface TacheResponseDto {
  id: number;
  code: string;
  titre: string;
  description: string | null;
  statut: string;
  priorite: string;
  dateEcheance: string | null;
  tempsEstime: number | null;
  tempsPasse: number | null;
  demarreeLe: string | null;
  depasseSprint: boolean;
  archivee?: boolean;
  dateArchivage?: string | null;
  idSprint: number | null;
  idProjet: number;
  assignations: UtilisateurAssigneDto[];
}

interface SousTacheDto {
  id: number;
  titre: string;
  fait: boolean;
}

interface PieceJointeDto {
  id: number;
  nomFichier: string;
  typeContenu: string;
  taille: number;
}

interface CommentaireDto {
  id: number;
  idAuteur: number;
  nomAuteur: string;
  contenu: string;
  dateCreation: string;
  pieceJointe?: PieceJointeDto | null;
}

interface TachePrecedenceDto {
  idTacheSource: number;
  idTacheCible: number;
  typeLien: string;
}

const STATUT_VERS_BACKEND: Record<StatutTache, string> = {
  a_faire: 'A_FAIRE',
  en_cours: 'EN_COURS',
  en_test: 'EN_TEST',
  bloquee: 'BLOQUEE',
  terminee: 'TERMINEE',
  reportee: 'REPORTEE',
};

const TYPE_LIEN_VERS_BACKEND: Record<TypeLienDependance, string> = {
  FD: 'FIN_DEBUT',
  DD: 'DEBUT_DEBUT',
  FF: 'FIN_FIN',
  DF: 'DEBUT_FIN',
};

const TYPE_LIEN_VERS_FRONTEND: Record<string, TypeLienDependance> = {
  FIN_DEBUT: 'FD',
  DEBUT_DEBUT: 'DD',
  FIN_FIN: 'FF',
  DEBUT_FIN: 'DF',
};

export interface NouvelleTache {
  projectId: string;
  titre: string;
  description?: string;
  priorite: Priorite;
  assigneIds: string[];
  sprintId?: string;
  echeance?: string;
  estimeHeures: number;
}

export interface ModificationTache {
  titre: string;
  description?: string;
  priorite: Priorite;
  assigneIds: string[];
  sprintId?: string;
  echeance?: string;
  estimeHeures: number;
}

export const KANBAN_COLUMNS: { statut: StatutTache; titre: string; couleur: string }[] = [
  { statut: 'a_faire', titre: 'À faire', couleur: '#98a2b3' },
  { statut: 'en_cours', titre: 'En cours', couleur: '#2a5bd7' },
  { statut: 'en_test', titre: 'En test', couleur: '#6941c6' },
  { statut: 'bloquee', titre: 'Bloquée', couleur: '#c0392b' },
  { statut: 'terminee', titre: 'Terminée', couleur: '#17876d' },
];

/** Conserve les champs qui n'existent pas (encore) côté backend — complexité IA — lors d'un rechargement. */
function versTask(dto: TacheResponseDto, existant?: Task): Task {
  return {
    id: String(dto.id),
    code: dto.code,
    projectId: String(dto.idProjet),
    sprintId: dto.idSprint != null ? String(dto.idSprint) : undefined,
    titre: dto.titre,
    description: dto.description ?? undefined,
    statut: dto.statut.toLowerCase() as StatutTache,
    priorite: dto.priorite.toLowerCase() as Priorite,
    assigneIds: dto.assignations.map((a) => String(a.idUtilisateur)),
    echeance: dto.dateEcheance ?? undefined,
    estimeHeures: dto.tempsEstime ?? 0,
    tempsPasseHeures: dto.tempsPasse ?? 0,
    demarreeLe: dto.demarreeLe ?? undefined,
    depasseSprint: dto.depasseSprint ?? false,
    archivee: dto.archivee ?? false,
    dateArchivage: dto.dateArchivage ?? undefined,
    complexite: existant?.complexite ?? 3,
    iaComplexiteSuggeree: existant?.iaComplexiteSuggeree,
    iaConfiance: existant?.iaConfiance,
    sousTaches: existant?.sousTaches ?? [],
    activite: existant?.activite ?? [],
  };
}

function versSousTache(dto: SousTacheDto, taskCode: string, index: number): SousTache {
  return { id: String(dto.id), code: `${taskCode}-${index + 1}`, titre: dto.titre, fait: dto.fait };
}

function versPieceJointeActivite(dto: PieceJointeDto): PieceJointeActivite {
  return { id: String(dto.id), nomFichier: dto.nomFichier, typeContenu: dto.typeContenu, taille: dto.taille };
}

function versActivite(dto: CommentaireDto): ActiviteTache {
  return {
    id: String(dto.id),
    type: 'commentaire',
    personneId: String(dto.idAuteur),
    quand: dto.dateCreation,
    texte: dto.contenu,
    pieceJointe: dto.pieceJointe ? versPieceJointeActivite(dto.pieceJointe) : undefined,
  };
}

function versDependance(dto: TachePrecedenceDto): DependanceTache {
  return {
    id: `${dto.idTacheSource}-${dto.idTacheCible}`,
    idTacheSource: String(dto.idTacheSource),
    idTacheCible: String(dto.idTacheCible),
    typeLien: TYPE_LIEN_VERS_FRONTEND[dto.typeLien],
  };
}

interface RequeteTache {
  titre: string;
  description: string | null;
  priorite: string;
  tempsEstime: number;
  idsUtilisateursAssignes: number[];
  idSprint: number | null;
  dateEcheance: string | null;
  statut?: string;
  tempsPasse?: number;
}

/** Tâches d'un projet (entité backend `Tache`, `/api/projets/{idProjet}/taches` + sous-ressources `/api/taches/{idTache}/...`). */
@Injectable({ providedIn: 'root' })
export class TasksService {
  private readonly http = inject(HttpClient);
  private readonly _tasks = signal<Task[]>([]);
  readonly tasks = this._tasks.asReadonly();

  private readonly _dependances = signal<DependanceTache[]>([]);
  readonly dependances = this._dependances.asReadonly();

  private readonly projetsCharges = new Set<string>();
  private readonly detailsCharges = new Set<string>();

  reload(projectId: string): void {
    firstValueFrom(this.http.get<Page<TacheResponseDto>>(`${environment.apiUrl}/projets/${projectId}/taches`, { params: { size: '1000' } }))
      .then((page) => {
        const existants = new Map(this._tasks().map((t) => [t.id, t]));
        const nouveaux = page.content.map((dto) => versTask(dto, existants.get(String(dto.id))));
        this._tasks.update((list) => [...list.filter((t) => t.projectId !== projectId), ...nouveaux]);
        this.projetsCharges.add(projectId);
      })
      .catch(() => undefined);
  }

  ensureLoaded(projectId: string): void {
    if (!this.projetsCharges.has(projectId)) this.reload(projectId);
  }

  /** Charge la sous-ressource complète (sous-tâches, activité, dépendances) d'une tâche — appelé à l'ouverture du détail. */
  ensureDetailLoaded(taskId: string): void {
    if (this.detailsCharges.has(taskId)) return;
    this.detailsCharges.add(taskId);
    const taskCode = this._tasks().find((t) => t.id === taskId)?.code ?? taskId;

    Promise.all([
      firstValueFrom(this.http.get<SousTacheDto[]>(`${environment.apiUrl}/taches/${taskId}/sous-taches`)),
      firstValueFrom(this.http.get<CommentaireDto[]>(`${environment.apiUrl}/taches/${taskId}/commentaires`)),
      firstValueFrom(this.http.get<TachePrecedenceDto[]>(`${environment.apiUrl}/taches/${taskId}/dependances`)),
    ])
      .then(([sousTaches, commentaires, dependances]) => {
        this._tasks.update((list) =>
          list.map((t) =>
            t.id === taskId
              ? { ...t, sousTaches: sousTaches.map((s, i) => versSousTache(s, taskCode, i)), activite: commentaires.map(versActivite) }
              : t,
          ),
        );
        const nouvelles = dependances.map(versDependance);
        this._dependances.update((list) => [...list.filter((d) => d.idTacheSource !== taskId && d.idTacheCible !== taskId), ...nouvelles]);
      })
      .catch(() => this.detailsCharges.delete(taskId));
  }

  byProject(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._tasks().filter((t) => t.projectId === projectId));
  }

  bySprint(sprintId: string) {
    return computed(() => this._tasks().filter((t) => t.sprintId === sprintId));
  }

  backlog(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._tasks().filter((t) => t.projectId === projectId && !t.sprintId));
  }

  byId(id: string) {
    return computed(() => this._tasks().find((t) => t.id === id));
  }

  async updateStatus(taskId: string, statut: StatutTache): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    await this.envoyerModification(taskId, { ...this.requeteDepuisTache(tache), statut: STATUT_VERS_BACKEND[statut] });
  }

  async moveToSprint(taskId: string, sprintId: string | undefined): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    await this.envoyerModification(taskId, { ...this.requeteDepuisTache(tache), idSprint: sprintId ? Number(sprintId) : null });
  }

  /** Retire une tâche de son sprint (remise dans le backlog) ; si elle n'était pas terminée, elle repasse à « à faire ». */
  async retirerDuSprint(taskId: string): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    const statut = tache.statut === 'terminee' ? tache.statut : 'a_faire';
    await this.envoyerModification(taskId, { ...this.requeteDepuisTache(tache), idSprint: null, statut: STATUT_VERS_BACKEND[statut] });
  }

  async addComment(taskId: string, _personneId: string, texte: string): Promise<void> {
    const dto = await firstValueFrom(this.http.post<CommentaireDto>(`${environment.apiUrl}/taches/${taskId}/commentaires`, { contenu: texte }));
    this._tasks.update((list) => list.map((t) => (t.id === taskId ? { ...t, activite: [...t.activite, versActivite(dto)] } : t)));
  }

  /** `texte` facultatif : une pièce jointe seule, sans commentaire, est acceptée. */
  async addCommentWithAttachment(taskId: string, _personneId: string, texte: string, fichier: File): Promise<void> {
    const formData = new FormData();
    if (texte) formData.append('contenu', texte);
    formData.append('fichier', fichier);
    const dto = await firstValueFrom(
      this.http.post<CommentaireDto>(`${environment.apiUrl}/taches/${taskId}/commentaires/avec-piece-jointe`, formData),
    );
    this._tasks.update((list) => list.map((t) => (t.id === taskId ? { ...t, activite: [...t.activite, versActivite(dto)] } : t)));
  }

  /** Télécharge la pièce jointe d'un commentaire (authentifiée via HttpClient, pas un lien direct). */
  async telechargerPieceJointe(taskId: string, idCommentaire: string, nomFichier: string): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`${environment.apiUrl}/taches/${taskId}/commentaires/${idCommentaire}/piece-jointe`, { responseType: 'blob' }),
    );
    const url = URL.createObjectURL(blob);
    const lien = document.createElement('a');
    lien.href = url;
    lien.download = nomFichier;
    lien.click();
    URL.revokeObjectURL(url);
  }

  async toggleSousTache(taskId: string, sousTacheId: string): Promise<void> {
    const dto = await firstValueFrom(this.http.post<SousTacheDto>(`${environment.apiUrl}/taches/${taskId}/sous-taches/${sousTacheId}/basculer`, {}));
    this._tasks.update((list) =>
      list.map((t) => (t.id === taskId ? { ...t, sousTaches: t.sousTaches.map((s) => (s.id === sousTacheId ? { ...s, fait: dto.fait } : s)) } : t)),
    );
  }

  async toggleAssignation(taskId: string, personneId: string): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    const assigneIds = tache.assigneIds.includes(personneId)
      ? tache.assigneIds.filter((id) => id !== personneId)
      : [...tache.assigneIds, personneId];
    await this.envoyerModification(taskId, { ...this.requeteDepuisTache(tache), idsUtilisateursAssignes: assigneIds.map(Number) });
  }

  async addSousTache(taskId: string, titre: string): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    const dto = await firstValueFrom(this.http.post<SousTacheDto>(`${environment.apiUrl}/taches/${taskId}/sous-taches`, { titre }));
    const sousTache = versSousTache(dto, tache.code, tache.sousTaches.length);
    this._tasks.update((list) => list.map((t) => (t.id === taskId ? { ...t, sousTaches: [...t.sousTaches, sousTache] } : t)));
  }

  async create(payload: NouvelleTache): Promise<Task> {
    const dto = await firstValueFrom(
      this.http.post<TacheResponseDto>(`${environment.apiUrl}/projets/${payload.projectId}/taches`, this.versRequete(payload)),
    );
    const tache = versTask(dto);
    this._tasks.update((list) => [...list, tache]);
    return tache;
  }

  async update(taskId: string, payload: ModificationTache): Promise<void> {
    await this.envoyerModification(taskId, this.versRequete(payload));
  }

  async remove(taskId: string): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    await firstValueFrom(this.http.delete<void>(`${environment.apiUrl}/projets/${tache.projectId}/taches/${taskId}`));
    this._tasks.update((list) => list.filter((t) => t.id !== taskId));
    this._dependances.update((list) => list.filter((d) => d.idTacheSource !== taskId && d.idTacheCible !== taskId));
  }

  /** Archive une tâche terminée (refusé par le backend sinon) : elle quitte le Kanban, ses données restent intactes. */
  async archiver(taskId: string): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    const dto = await firstValueFrom(this.http.post<TacheResponseDto>(`${environment.apiUrl}/projets/${tache.projectId}/taches/${taskId}/archiver`, {}));
    this._tasks.update((list) => list.map((t) => (t.id === taskId ? versTask(dto, t) : t)));
  }

  /** Désarchive une tâche : elle revient dans le Kanban (colonne « Terminée »). */
  async restaurer(taskId: string): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    const dto = await firstValueFrom(this.http.post<TacheResponseDto>(`${environment.apiUrl}/projets/${tache.projectId}/taches/${taskId}/restaurer`, {}));
    this._tasks.update((list) => list.map((t) => (t.id === taskId ? versTask(dto, t) : t)));
  }

  async removeSousTache(taskId: string, sousTacheId: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${environment.apiUrl}/taches/${taskId}/sous-taches/${sousTacheId}`));
    this._tasks.update((list) =>
      list.map((t) => (t.id === taskId ? { ...t, sousTaches: t.sousTaches.filter((s) => s.id !== sousTacheId) } : t)),
    );
  }

  dependancesBloquantes(taskId: string) {
    return computed(() => this._dependances().filter((d) => d.idTacheCible === taskId));
  }

  dependancesBloquees(taskId: string) {
    return computed(() => this._dependances().filter((d) => d.idTacheSource === taskId));
  }

  async creerDependance(idTacheSource: string, idTacheCible: string, typeLien: TypeLienDependance): Promise<void> {
    const dto = await firstValueFrom(
      this.http.post<TachePrecedenceDto>(`${environment.apiUrl}/taches/${idTacheSource}/dependances`, {
        idTacheCible: Number(idTacheCible),
        typeLien: TYPE_LIEN_VERS_BACKEND[typeLien],
      }),
    );
    this._dependances.update((list) => [...list, versDependance(dto)]);
  }

  async supprimerDependance(id: string): Promise<void> {
    const [idTacheSource, idTacheCible] = id.split('-');
    await firstValueFrom(this.http.delete<void>(`${environment.apiUrl}/taches/${idTacheSource}/dependances/${idTacheCible}`));
    this._dependances.update((list) => list.filter((d) => d.id !== id));
  }

  private versRequete(payload: NouvelleTache | ModificationTache): RequeteTache {
    return {
      titre: payload.titre,
      description: payload.description ?? null,
      priorite: payload.priorite.toUpperCase(),
      tempsEstime: payload.estimeHeures,
      idsUtilisateursAssignes: payload.assigneIds.map(Number),
      idSprint: payload.sprintId ? Number(payload.sprintId) : null,
      dateEcheance: payload.echeance ?? null,
    };
  }

  /** Reconstruit la requête complète attendue par le backend à partir d'une tâche en cache (utilisé pour ne modifier qu'un seul champ). */
  private requeteDepuisTache(tache: Task): RequeteTache {
    return {
      titre: tache.titre,
      description: tache.description ?? null,
      priorite: tache.priorite.toUpperCase(),
      tempsEstime: tache.estimeHeures,
      idsUtilisateursAssignes: tache.assigneIds.map(Number),
      idSprint: tache.sprintId ? Number(tache.sprintId) : null,
      dateEcheance: tache.echeance ?? null,
      statut: STATUT_VERS_BACKEND[tache.statut],
    };
  }

  private async envoyerModification(taskId: string, requete: RequeteTache): Promise<void> {
    const tache = this._tasks().find((t) => t.id === taskId);
    if (!tache) return;
    const dto = await firstValueFrom(this.http.put<TacheResponseDto>(`${environment.apiUrl}/projets/${tache.projectId}/taches/${taskId}`, requete));
    this._tasks.update((list) => list.map((t) => (t.id === taskId ? versTask(dto, t) : t)));
  }
}
