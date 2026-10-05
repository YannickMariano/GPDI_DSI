export enum Role {
  Administrateur = 'administrateur',
  DirecteurDsi = 'directeur_dsi',
  ChefProjet = 'chef_projet',
  MembreEquipe = 'membre_equipe',
  DirectionMetier = 'direction_metier',
}

export const ROLE_LABELS: Record<Role, string> = {
  [Role.Administrateur]: 'Administrateur',
  [Role.DirecteurDsi]: 'Directeur DSI',
  [Role.ChefProjet]: 'Chef de projet',
  [Role.MembreEquipe]: 'Membre équipe',
  [Role.DirectionMetier]: 'Direction métier',
};

/** Couleur d'accent par rôle (cartes de rôle, pastilles) — alignée sur les tokens de src/styles/tokens.css. */
export const ROLE_STYLE: Record<Role, { color: string; bg: string }> = {
  [Role.Administrateur]: { color: 'var(--color-ai)', bg: 'var(--color-ai-bg)' },
  [Role.DirecteurDsi]: { color: 'var(--color-action)', bg: 'var(--color-action-bg)' },
  [Role.ChefProjet]: { color: 'var(--color-success-text)', bg: 'var(--color-success-bg)' },
  [Role.MembreEquipe]: { color: 'var(--color-text-secondary)', bg: 'var(--color-surface-alt)' },
  [Role.DirectionMetier]: { color: 'var(--color-alert)', bg: 'var(--color-alert-bg)' },
};

/** Ligne de la table `roles` en base (entité backend `Role` : id, nom, description). */
export interface RoleRecord {
  id: number;
  nom: string;
  description: string;
}

export function defaultRouteForRole(role: Role): string {
  switch (role) {
    case Role.DirecteurDsi:
      return '/tableau-de-bord';
    case Role.Administrateur:
      return '/administration';
    case Role.MembreEquipe:
      return '/mon-travail';
    case Role.DirectionMetier:
      return '/espace-client';
    case Role.ChefProjet:
    default:
      return '/projets';
  }
}
