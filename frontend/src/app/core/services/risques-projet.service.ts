import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@environments/environment';
import { SANS_TOAST_ERREUR } from '@core/interceptors/error.interceptor';
import { StatutRisque } from '@core/models/risk.model';
import { TasksService } from './tasks.service';

interface SolutionRisqueResponse {
  id: number;
  libelle: string;
  justification: string | null;
  gain: string | null;
  typeAction: string;
  statut: string;
  codeTache: string | null;
  motifRejet: string | null;
}

interface RisqueResponse {
  id: number;
  code: string;
  libelle: string;
  parade: string | null;
  analyse: string | null;
  probabilite: number;
  impact: number;
  statut: string;
  dateDetection: string;
  idPorteur: number | null;
  solutions: SolutionRisqueResponse[];
}

interface SolutionTraiteeResponse {
  id: number;
  libelle: string;
  typeAction: string;
  idTache: number | null;
  codeTache: string | null;
  suivi: boolean;
  remarque: string | null;
}

interface TraitementRisqueResponse {
  idRisque: number;
  statut: string;
  solutions: SolutionTraiteeResponse[];
}

interface ResultatDetectionRisqueResponse {
  idAnalyse: number;
  nombreNouveauxRisques: number;
  nombreRisquesReevalues: number;
  nombreRisquesClos: number;
  synthese: string | null;
}

/** Proposée : en attente d'un choix. Appliquée : retenue et exécutée. Rejetée : écartée (explicitement ou non retenue lors d'un choix). */
export type StatutSolutionRisque = 'proposee' | 'appliquee' | 'rejetee';

/** Solution proposée par l'assistant IA pour un risque : reste visible (lecture seule) une fois le risque traité. */
export interface SolutionRisque {
  id: string;
  libelle: string;
  gain: string;
  justification?: string;
  typeAction: string;
  statut: StatutSolutionRisque;
  codeTache?: string;
  motifRejet?: string;
}

/** Risque d'un projet, détecté par l'assistant IA (entité backend `Risque`, `/api/projets/{idProjet}/risques`). */
export interface RisqueProjet {
  id: string;
  code: string;
  projectId: string;
  libelle: string;
  parade: string;
  analyse: string;
  probabilite: number;
  impact: number;
  porteurId: string;
  statut: StatutRisque;
  detection: string;
  solutions: SolutionRisque[];
}

export interface ResultatDetection {
  nouveaux: number;
  reevalues: number;
  clos: number;
  synthese: string;
}

export interface SolutionTraitee {
  libelle: string;
  codeTache?: string;
  remarque?: string;
}

function versSolution(dto: SolutionRisqueResponse): SolutionRisque {
  return {
    id: String(dto.id),
    libelle: dto.libelle,
    gain: dto.gain ?? '',
    justification: dto.justification ?? undefined,
    typeAction: dto.typeAction,
    statut: dto.statut.toLowerCase() as StatutSolutionRisque,
    codeTache: dto.codeTache ?? undefined,
    motifRejet: dto.motifRejet ?? undefined,
  };
}

function versRisque(dto: RisqueResponse, projectId: string): RisqueProjet {
  return {
    id: String(dto.id),
    code: dto.code,
    projectId,
    libelle: dto.libelle,
    parade: dto.parade ?? '',
    analyse: dto.analyse ?? '',
    probabilite: dto.probabilite,
    impact: dto.impact,
    porteurId: dto.idPorteur != null ? String(dto.idPorteur) : '',
    statut: dto.statut.toLowerCase() as StatutRisque,
    detection: dto.dateDetection,
    solutions: dto.solutions.map(versSolution),
  };
}

/**
 * Risques d'un projet gérés avec l'assistant IA. La détection et la recherche de solutions interrogent un modèle
 * local (Ollama) et peuvent durer plusieurs minutes : leur état « en cours » vit ici, pour survivre à un changement d'onglet.
 */
@Injectable({ providedIn: 'root' })
export class RisquesProjetService {
  private readonly http = inject(HttpClient);
  private readonly tasksService = inject(TasksService);

  private readonly _risques = signal<RisqueProjet[]>([]);
  /** Risques de tous les projets déjà chargés (vue portefeuille : appeler `ensureLoaded` pour chaque projet). */
  readonly risques = this._risques.asReadonly();
  private readonly _detectionsEnCours = signal<ReadonlySet<string>>(new Set());
  private readonly _recherchesEnCours = signal<ReadonlySet<string>>(new Set());
  private readonly _applicationsEnCours = signal<ReadonlySet<string>>(new Set());

  private readonly projetsCharges = new Set<string>();

  /** Recharge les risques d'un projet depuis le backend. `silencieux` : pas de message d'erreur (chargement d'arrière-plan, ex. droits insuffisants). */
  reload(projectId: string, silencieux = false): Promise<void> {
    const context = silencieux ? new HttpContext().set(SANS_TOAST_ERREUR, true) : undefined;
    return firstValueFrom(this.http.get<RisqueResponse[]>(`${environment.apiUrl}/projets/${projectId}/risques`, { context }))
      .then((dtos) => {
        const nouveaux = dtos.map((dto) => versRisque(dto, projectId));
        this._risques.update((list) => [...list.filter((r) => r.projectId !== projectId), ...nouveaux]);
        this.projetsCharges.add(projectId);
      })
      .catch(() => undefined);
  }

  ensureLoaded(projectId: string): void {
    if (!this.projetsCharges.has(projectId)) void this.reload(projectId, true);
  }

  byProject(projectId: string) {
    this.ensureLoaded(projectId);
    return computed(() => this._risques().filter((r) => r.projectId === projectId));
  }

  estEnDetection(projectId: string): boolean {
    return this._detectionsEnCours().has(projectId);
  }

  estEnRecherche(risqueId: string): boolean {
    return this._recherchesEnCours().has(risqueId);
  }

  estEnApplication(risqueId: string): boolean {
    return this._applicationsEnCours().has(risqueId);
  }

  /** Lance la détection des risques du projet par l'IA (jusqu'à plusieurs minutes), puis recharge la liste. */
  async detecter(projectId: string): Promise<ResultatDetection> {
    this.marquer(this._detectionsEnCours, projectId, true);
    try {
      const dto = await firstValueFrom(this.http.post<ResultatDetectionRisqueResponse>(`${environment.apiUrl}/projets/${projectId}/risques/detection`, {}));
      await this.reload(projectId);
      return { nouveaux: dto.nombreNouveauxRisques, reevalues: dto.nombreRisquesReevalues, clos: dto.nombreRisquesClos, synthese: dto.synthese ?? '' };
    } finally {
      this.marquer(this._detectionsEnCours, projectId, false);
    }
  }

  /** Demande les solutions de l'IA pour un risque ouvert (déjà calculées si elles existent, sinon jusqu'à plusieurs minutes). */
  async suggerer(risque: RisqueProjet): Promise<void> {
    this.marquer(this._recherchesEnCours, risque.id, true);
    try {
      const dtos = await firstValueFrom(
        this.http.post<SolutionRisqueResponse[]>(`${environment.apiUrl}/projets/${risque.projectId}/risques/${risque.id}/suggestions`, {}),
      );
      this.modifier(risque.id, (r) => ({ ...r, solutions: dtos.map(versSolution) }));
    } finally {
      this.marquer(this._recherchesEnCours, risque.id, false);
    }
  }

  /**
   * Applique les solutions cochées (tout ou rien) : le risque passe « sous contrôle » et des tâches sont
   * créées ou réassignées. Recharge le risque depuis le backend pour récupérer l'historique complet des
   * recommandations (appliquées et rejetées), affiché ensuite en lecture seule.
   */
  async appliquer(risque: RisqueProjet, idsSolutions: string[]): Promise<SolutionTraitee[]> {
    this.marquer(this._applicationsEnCours, risque.id, true);
    try {
      const dto = await firstValueFrom(
        this.http.post<TraitementRisqueResponse>(`${environment.apiUrl}/projets/${risque.projectId}/risques/${risque.id}/appliquer`, {
          idsSolutions: idsSolutions.map(Number),
        }),
      );
      await this.reload(risque.projectId);
      // Les solutions ont créé ou réassigné des tâches : le cache des tâches du projet est périmé.
      this.tasksService.reload(risque.projectId);
      return dto.solutions.map((s) => ({ libelle: s.libelle, codeTache: s.codeTache ?? undefined, remarque: s.remarque ?? undefined }));
    } finally {
      this.marquer(this._applicationsEnCours, risque.id, false);
    }
  }

  /** Rejette l'analyse : recharge le risque pour afficher l'historique des recommandations (désormais toutes rejetées) en lecture seule. */
  async rejeter(risque: RisqueProjet): Promise<void> {
    await firstValueFrom(
      this.http.post<TraitementRisqueResponse>(`${environment.apiUrl}/projets/${risque.projectId}/risques/${risque.id}/rejeter`, {}),
    );
    await this.reload(risque.projectId);
  }

  private modifier(risqueId: string, transformation: (risque: RisqueProjet) => RisqueProjet): void {
    this._risques.update((list) => list.map((r) => (r.id === risqueId ? transformation(r) : r)));
  }

  private marquer(ensemble: ReturnType<typeof signal<ReadonlySet<string>>>, id: string, actif: boolean): void {
    ensemble.update((courant) => {
      const copie = new Set(courant);
      if (actif) copie.add(id);
      else copie.delete(id);
      return copie;
    });
  }
}
