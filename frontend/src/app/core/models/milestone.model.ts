export type StatutJalon = 'atteint' | 'a_risque' | 'planifie' | 'annule';
export type StatutLivrable = 'a_livrer' | 'livre' | 'rejete';

export interface Livrable {
  id: string;
  description: string;
  type: string;
  dateLivraisonPrevue: string;
  statut: StatutLivrable;
  dateLivraison?: string;
  dateRejet?: string;
  motifRejet?: string;
}

export interface Milestone {
  id: string;
  projectId: string;
  nom: string;
  date: string;
  statut: StatutJalon;
  description: string;
  livrables: Livrable[];
  alerte?: string;
}
