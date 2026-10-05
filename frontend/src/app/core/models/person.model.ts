import { Role } from './role.model';

export type StatutCompte = 'actif' | 'suspendu';

export interface Person {
  id: string;
  nom: string;
  /** Prénom brut (non tronqué), tel qu'enregistré côté DSI — utilisé pour pré-remplir le formulaire de modification. */
  prenom: string;
  /** Nom de famille brut (non tronqué). */
  nomUsuel: string;
  initiales: string;
  avatarBg: string;
  role: Role;
  direction: string;
  email: string;
  telephone?: string;
  matricule: string;
  statut: StatutCompte;
}
