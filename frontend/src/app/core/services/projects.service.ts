import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { Project, StatutProjet } from '@core/models/project.model';
import { AuthUser } from '@core/models/user.model';
import { Role } from '@core/models/role.model';
import { Priorite, Task } from '@core/models/task.model';
import { EquipesService } from './equipes.service';
import { MilestonesService } from './milestones.service';
import { SprintsService } from './sprints.service';
import { TasksService } from './tasks.service';

interface Page<T> {
  content: T[];
}

interface ProjetResponse {
  id: number;
  titre: string;
  codeReference: string;
  description: string;
  dateDebut: string;
  dateFin: string;
  dateCloture: string | null;
  statut: string;
  priorite: string;
  progression: number;
  archive: boolean;
  idResponsable: number;
  nomResponsable: string;
  idEquipe: number | null;
  nomEquipe: string | null;
  direction: string | null;
  sante: string;
}

export interface ProjectPayload {
  nom: string;
  description: string;
  direction: string;
  responsableId: string;
  priorite: Priorite;
  dateDebut: string;
  dateFin: string;
  /** Optionnel : équipe DSI organisationnelle rattachée au projet (`Projet.idEquipe` côté backend). */
  equipeId?: string;
}

function versProject(dto: ProjetResponse): Project {
  return {
    id: String(dto.id),
    nom: dto.titre,
    description: dto.description ?? '',
    direction: dto.direction ?? '',
    codeReference: dto.codeReference,
    responsableId: String(dto.idResponsable),
    // dto.statut/priorite ne devraient jamais être absents (valeurs par défaut côté entité), mais une ligne
    // insérée hors de l'application (édition directe en base) peut les laisser à null : on ne doit pas
    // planter tout le chargement des projets pour autant.
    statut: (dto.statut?.toLowerCase() as StatutProjet) ?? 'en_preparation',
    priorite: (dto.priorite?.toLowerCase() as Priorite) ?? 'normale',
    sante: dto.sante as Project['sante'],
    avancement: dto.progression ?? 0,
    // Pas encore calculables : Sprints/Jalons/Tâches ne sont pas reliés aux projets réels.
    sprintActuelIndex: 0,
    sprintTotal: 0,
    heuresConsommees: 0,
    dateDebut: dto.dateDebut,
    dateFin: dto.dateFin,
    dateCloture: dto.dateCloture ?? undefined,
    prochainJalon: undefined,
    equipeId: dto.idEquipe != null ? String(dto.idEquipe) : undefined,
    nomEquipe: dto.nomEquipe ?? undefined,
  };
}

function versRequest(payload: ProjectPayload) {
  return {
    titre: payload.nom,
    description: payload.description,
    dateDebut: payload.dateDebut,
    dateFin: payload.dateFin,
    priorite: payload.priorite.toUpperCase(),
    idResponsable: Number(payload.responsableId),
    idEquipe: payload.equipeId ? Number(payload.equipeId) : null,
  };
}

/** Projets (entité backend `Projet`, `/api/projets`). */
@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly http = inject(HttpClient);
  private readonly sprintsService = inject(SprintsService);
  private readonly tasksService = inject(TasksService);
  private readonly milestonesService = inject(MilestonesService);
  private readonly equipesService = inject(EquipesService);

  private readonly _projects = signal<Project[]>([]);
  /** Projets du backend, enrichis des indicateurs calculés côté frontend (sprint, heures, prochain jalon) pour les projets dont les données sont chargées. */
  readonly projects = computed(() => this.avecIndicateurs(this._projects()));
  readonly projectIds = computed(() => this._projects().map((p) => p.id));
  readonly totalActifs = computed(() => this.projects().filter((p) => p.statut === 'en_cours').length);

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<Page<ProjetResponse>>(`${environment.apiUrl}/projets`, { params: { size: '1000' } }))
      .then((page) => this._projects.set(page.content.map(versProject)))
      .catch(() => undefined);
  }

  /**
   * Charge sprints, tâches et jalons de chaque projet (une seule fois) pour alimenter les indicateurs des projets.
   * `projetIds` limite le chargement à ces projets (ex. ceux réellement accessibles à l'utilisateur) ; absent = tous les projets.
   */
  chargerIndicateurs(projetIds?: string[]): void {
    const ids = projetIds ?? this._projects().map((p) => p.id);
    for (const id of ids) {
      this.sprintsService.ensureLoaded(id);
      this.tasksService.ensureLoaded(id);
      this.milestonesService.ensureLoaded(id);
    }
  }

  private avecIndicateurs(projets: Project[]): Project[] {
    const sprints = this.sprintsService.sprints();
    const taches = this.tasksService.tasks();
    const jalons = this.milestonesService.milestones();

    return projets.map((projet) => {
      const sprintsProjet = sprints.filter((s) => s.projectId === projet.id).sort((a, b) => a.dateDebut.localeCompare(b.dateDebut));
      const indexCourant = sprintsProjet.findIndex((s) => s.statut === 'en_cours');
      const prochainJalon = jalons
        .filter((j) => j.projectId === projet.id && j.statut === 'planifie')
        .map((j) => j.date)
        .sort()[0];

      return {
        ...projet,
        sprintTotal: sprintsProjet.length,
        sprintActuelIndex: indexCourant >= 0 ? indexCourant + 1 : sprintsProjet.filter((s) => s.statut === 'cloture').length,
        heuresConsommees: taches.filter((t) => t.projectId === projet.id).reduce((somme, t) => somme + t.tempsPasseHeures, 0),
        prochainJalon,
      };
    });
  }

  byId(id: string) {
    return computed(() => this.projects().find((p) => p.id === id));
  }

  /**
   * Projets auxquels l'utilisateur est rattaché : responsable pour un Chef de projet ; pour un Membre
   * équipe, appartenance à l'équipe DSI rattachée au projet (`Projet.idEquipe`) ou tâches assignées.
   */
  pourUtilisateur(user: AuthUser, taches: Task[]): Project[] {
    const tous = this.projects();

    if (user.role === Role.ChefProjet) {
      const miens = tous.filter((p) => p.responsableId === user.id);
      return miens.length > 0 ? miens : tous;
    }

    if (user.role === Role.MembreEquipe) {
      const equipeIdsDeLUtilisateur = new Set(
        this.equipesService.teams()
          .filter((e) => e.chefId === user.id || e.membreIds.includes(user.id))
          .map((e) => e.id),
      );
      const parEquipe = new Set(tous.filter((p) => p.equipeId && equipeIdsDeLUtilisateur.has(p.equipeId)).map((p) => p.id));
      const parTaches = new Set(taches.filter((t) => t.assigneIds.includes(user.id)).map((t) => t.projectId));
      return tous.filter((p) => parEquipe.has(p.id) || parTaches.has(p.id));
    }

    return tous;
  }

  async create(payload: ProjectPayload): Promise<Project> {
    const dto = await firstValueFrom(this.http.post<ProjetResponse>(`${environment.apiUrl}/projets`, versRequest(payload)));
    const projet = versProject(dto);
    this._projects.update((list) => [...list, projet]);
    return projet;
  }

  async update(id: string, payload: ProjectPayload): Promise<void> {
    const dto = await firstValueFrom(this.http.put<ProjetResponse>(`${environment.apiUrl}/projets/${id}`, versRequest(payload)));
    const projet = versProject(dto);
    this._projects.update((list) => list.map((p) => (p.id === id ? projet : p)));
  }

  async archiver(id: string): Promise<void> {
    const dto = await firstValueFrom(this.http.post<ProjetResponse>(`${environment.apiUrl}/projets/${id}/archiver`, {}));
    this._projects.update((list) => list.map((p) => (p.id === id ? versProject(dto) : p)));
  }

  async suspendre(id: string): Promise<void> {
    const dto = await firstValueFrom(this.http.post<ProjetResponse>(`${environment.apiUrl}/projets/${id}/suspendre`, {}));
    this._projects.update((list) => list.map((p) => (p.id === id ? versProject(dto) : p)));
  }

  async cloturer(id: string): Promise<void> {
    const dto = await firstValueFrom(this.http.post<ProjetResponse>(`${environment.apiUrl}/projets/${id}/cloturer`, {}));
    this._projects.update((list) => list.map((p) => (p.id === id ? versProject(dto) : p)));
  }

  attentionRequise() {
    return computed(() =>
      [...this.projects()]
        .filter((p) => p.sante !== 'sain')
        .sort((a, b) => this.criticiteScore(b) - this.criticiteScore(a)),
    );
  }

  private criticiteScore(p: Project): number {
    return p.sante === 'a_risque' ? 2 : p.sante === 'sous_surveillance' ? 1 : 0;
  }
}
