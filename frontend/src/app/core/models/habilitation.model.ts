import { Role } from './role.model';

/** Module 0 — matrice des habilitations par fonctionnalité, portée du design (Écriture / Lecture / Aucun). */
export type NiveauAcces = 'ecriture' | 'lecture' | 'aucun';

export interface PermissionRow {
  fonction: string;
  module: string;
  acces: Record<Role, NiveauAcces>;
}

export interface HabilitationLigneRequest {
  fonction: string;
  roleNom: string;
  niveau: NiveauAcces;
}
