import { Priorite } from './task.model';

export type SanteProjet = 'sain' | 'sous_surveillance' | 'a_risque';
export type StatutProjet = 'en_preparation' | 'en_cours' | 'suspendu' | 'cloture' | 'archive';

export interface Project {
  id: string;
  nom: string;
  description: string;
  direction: string;
  codeReference: string;
  responsableId: string;
  statut: StatutProjet;
  priorite: Priorite;
  sante: SanteProjet;
  avancement: number;
  sprintActuelIndex: number;
  sprintTotal: number;
  dateDebut: string;
  dateFin: string;
  dateCloture?: string;
  prochainJalon?: string;
  heuresConsommees: number;
  equipeId?: string;
  nomEquipe?: string;
}
