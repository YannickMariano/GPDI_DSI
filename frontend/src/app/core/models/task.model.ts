export type StatutTache = 'a_faire' | 'en_cours' | 'en_test' | 'bloquee' | 'terminee' | 'reportee';
export type Priorite = 'critique' | 'haute' | 'normale' | 'basse';
export type TypeActivite = 'commentaire' | 'systeme' | 'temps';
export type TypeLienDependance = 'FD' | 'DD' | 'FF' | 'DF';

export interface DependanceTache {
  id: string;
  /** Tâche qui doit avancer d'abord. */
  idTacheSource: string;
  /** Tâche qui attend la tâche source selon le type de lien. */
  idTacheCible: string;
  typeLien: TypeLienDependance;
}

export interface SousTache {
  id: string;
  code: string;
  titre: string;
  fait: boolean;
  personneId?: string;
}

export interface PieceJointeActivite {
  id: string;
  nomFichier: string;
  typeContenu: string;
  taille: number;
}

export interface ActiviteTache {
  id: string;
  type: TypeActivite;
  personneId?: string;
  quand: string;
  texte: string;
  pieceJointe?: PieceJointeActivite;
}

export interface Task {
  id: string;
  code: string;
  projectId: string;
  sprintId?: string;
  titre: string;
  description?: string;
  statut: StatutTache;
  priorite: Priorite;
  assigneIds: string[];
  echeance?: string;
  estimeHeures: number;
  tempsPasseHeures: number;
  /** Horodatage de démarrage du chronomètre (saisie de temps) ; absent si la tâche n'est pas en cours de saisie. */
  demarreeLe?: string;
  /** Tâche terminée après la date de fin de son sprint (la clôture reste permise, elle est seulement signalée). */
  depasseSprint: boolean;
  complexite: number;
  iaComplexiteSuggeree?: number;
  iaConfiance?: number;
  sousTaches: SousTache[];
  activite: ActiviteTache[];
}
