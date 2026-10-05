import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { Person } from '@core/models/person.model';
import { Role } from '@core/models/role.model';
import { avatarColorFor } from '@shared/utils/avatar-color';
import { UtilisateurResponse, initialesUtilisateur, nomAffiche, roleDepuisNom } from '@shared/utils/utilisateur-view';
import { SANS_TOAST_ERREUR } from '@core/interceptors/error.interceptor';
import { RolesService } from './roles.service';

interface Page<T> {
  content: T[];
}

export interface PersonCreationPayload {
  prenom: string;
  nom: string;
  matricule: string;
  email: string;
  telephone?: string;
  role: Role;
  direction: string;
  motDePasse: string;
}

export type PersonModificationPayload = Omit<PersonCreationPayload, 'motDePasse'> & { motDePasse?: string };

/** Comptes utilisateurs applicatifs (entité backend `Utilisateur`, `/api/utilisateurs`). */
@Injectable({ providedIn: 'root' })
export class PeopleService {
  private readonly http = inject(HttpClient);
  private readonly rolesService = inject(RolesService);

  private readonly _people = signal<Person[]>([]);
  readonly people = this._people.asReadonly();
  readonly activeCount = computed(() => this._people().filter((p) => p.statut === 'actif').length);
  readonly suspendedCount = computed(() => this._people().filter((p) => p.statut === 'suspendu').length);

  constructor() {
    this.reload();
  }

  reload(): void {
    firstValueFrom(this.http.get<Page<UtilisateurResponse>>(`${environment.apiUrl}/utilisateurs`, { params: { size: '1000', sort: 'nom,asc' } }))
      .then((page) => this._people.set(page.content.map((dto) => this.versPerson(dto)).filter((p): p is Person => p !== null)))
      .catch(() => undefined);
  }

  byId(id: string): Person | undefined {
    return this._people().find((p) => p.id === id);
  }

  byMatricule(matricule: string): Person | undefined {
    return this._people().find((p) => p.matricule === matricule);
  }

  byRole(role: Person['role']): Person[] {
    return this._people().filter((p) => p.role === role);
  }

  async create(payload: PersonCreationPayload): Promise<Person> {
    const idRole = await this.resolveIdRole(payload.role);
    const dto = await firstValueFrom(
      this.http.post<UtilisateurResponse>(`${environment.apiUrl}/utilisateurs`, {
        matricule: payload.matricule,
        nom: payload.nom,
        prenom: payload.prenom,
        email: payload.email,
        telephone: payload.telephone || null,
        motDePasse: payload.motDePasse,
        specialite: payload.direction,
        idRole,
      }),
    );
    const personne = this.versPerson(dto);
    if (!personne) throw new Error('Compte créé mais rôle non reconnu.');
    this._people.update((list) => [...list, personne]);
    return personne;
  }

  async update(id: string, payload: PersonModificationPayload): Promise<void> {
    const idRole = await this.resolveIdRole(payload.role);
    const dto = await firstValueFrom(
      this.http.put<UtilisateurResponse>(`${environment.apiUrl}/utilisateurs/${id}`, {
        matricule: payload.matricule,
        nom: payload.nom,
        prenom: payload.prenom,
        email: payload.email,
        telephone: payload.telephone || null,
        motDePasse: payload.motDePasse || null,
        specialite: payload.direction,
        idRole,
      }),
    );
    const personne = this.versPerson(dto);
    if (!personne) return;
    this._people.update((list) => list.map((p) => (p.id === id ? personne : p)));
  }

  /**
   * `detacherTaches` : si la suppression est refusée (409, compte encore assigné à des tâches), relancer
   * avec `true` pour le retirer de ces tâches puis le supprimer. Le premier essai (409 attendu, géré par
   * l'appelant via un second dialogue de confirmation) n'affiche pas le toast d'erreur générique.
   */
  async delete(id: string, detacherTaches = false): Promise<void> {
    const context = detacherTaches ? undefined : new HttpContext().set(SANS_TOAST_ERREUR, true);
    await firstValueFrom(
      this.http.delete<void>(`${environment.apiUrl}/utilisateurs/${id}`, { params: { detacherTaches: String(detacherTaches) }, context }),
    );
    this._people.update((list) => list.filter((p) => p.id !== id));
  }

  async setStatus(id: string, statut: Person['statut']): Promise<void> {
    const action = statut === 'actif' ? 'activer' : 'suspendre';
    const dto = await firstValueFrom(this.http.post<UtilisateurResponse>(`${environment.apiUrl}/utilisateurs/${id}/${action}`, {}));
    const personne = this.versPerson(dto);
    if (!personne) return;
    this._people.update((list) => list.map((p) => (p.id === id ? personne : p)));
  }

  private async resolveIdRole(role: Role): Promise<number> {
    await this.rolesService.ensureLoaded();
    const record = this.rolesService.byNom(role);
    if (!record) throw new Error(`Rôle "${role}" absent de la base des rôles applicatifs.`);
    return record.id;
  }

  private versPerson(dto: UtilisateurResponse): Person | null {
    const role = roleDepuisNom(dto.nomRole);
    if (!role) return null;
    return {
      id: String(dto.id),
      nom: nomAffiche(dto.prenom, dto.nom),
      prenom: dto.prenom,
      nomUsuel: dto.nom,
      initiales: initialesUtilisateur(dto.prenom, dto.nom),
      avatarBg: avatarColorFor(dto.matricule),
      role,
      direction: dto.specialite ?? '',
      email: dto.email,
      telephone: dto.telephone ?? undefined,
      matricule: dto.matricule,
      statut: dto.actif ? 'actif' : 'suspendu',
    };
  }
}
