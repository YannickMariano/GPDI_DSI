export type Criticite = 'critique' | 'eleve' | 'modere' | 'faible';

/** Ouvert : suggéré par l'IA, en attente de traitement. Sous contrôle : solutions appliquées. Rejeté : analyse écartée. Clos : criticité retombée à faible. */
export type StatutRisque = 'ouvert' | 'sous_controle' | 'rejete' | 'clos';

export function criticiteRisque(probabilite: number, impact: number): Criticite {
  const score = probabilite * impact;
  if (score >= 15) return 'critique';
  if (score >= 10) return 'eleve';
  if (score >= 5) return 'modere';
  return 'faible';
}
