import { Role } from './role.model';

export interface AuthUser {
  id: string;
  matricule: string;
  nom: string;
  initiales: string;
  avatarBg: string;
  role: Role;
  direction: string;
  projetActifId?: string;
}

export interface LoginCredentials {
  /** Matricule ou adresse e-mail de l'utilisateur. */
  identifiant: string;
  motDePasse: string;
  gardeSession: boolean;
}

export interface LoginResult {
  success: boolean;
  user?: AuthUser;
  message?: string;
  tentativesRestantes?: number;
}
