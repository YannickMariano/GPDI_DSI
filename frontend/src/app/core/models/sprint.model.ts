export type StatutSprint = 'planifie' | 'en_cours' | 'cloture';

export interface Sprint {
  id: string;
  projectId: string;
  nom: string;
  statut: StatutSprint;
  dateDebut: string;
  dateFin: string;
  objectif: string;
  capaciteHeures: number;
}
