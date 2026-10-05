import { Role } from '@core/models/role.model';

/** Forme renvoyée par le backend pour un utilisateur (`UtilisateurResponse`, `MembreEquipeResponse` partagent ces champs). */
export interface UtilisateurResponse {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  specialite: string | null;
  actif: boolean;
  idRole: number | null;
  nomRole: string | null;
}

/** Convention d'affichage GPDI pour un nom d'agent : initiale du prénom + nom de famille (ex. « K. Traoré »). */
export function nomAffiche(prenom: string, nom: string): string {
  return `${prenom.charAt(0).toUpperCase()}. ${nom}`;
}

export function initialesUtilisateur(prenom: string, nom: string): string {
  return (prenom.charAt(0) + nom.charAt(0)).toUpperCase();
}

/** Résout le rôle métier à partir du libellé renvoyé par l'API. `undefined` si absent ou non reconnu (à charge de l'appelant de décider si c'est bloquant). */
export function roleDepuisNom(nomRole: string | null): Role | undefined {
  return Object.values(Role).find((r) => r === nomRole);
}
