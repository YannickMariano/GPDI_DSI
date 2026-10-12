import { Role } from '@core/models/role.model';

export interface RailItem {
  icone: string;
  libelle: string;
  route: string;
  badgeKey?: 'monTravail' | 'projetsActifs' | 'ia';
  /** Si vrai, l'item n'est actif que sur une correspondance exacte de route (pas en préfixe). */
  exact?: boolean;
  /** Préfixes d'URL supplémentaires qui doivent aussi activer cet item (ex. une redirection vers un sous-espace). */
  activePrefixes?: string[];
}

/**
 * Correspond au modèle de rôles de l'écran Administration (source d'autorité RBAC) :
 * Administration est réservée à l'Administrateur, à l'exception du Journal d'accès également
 * ouvert au Directeur DSI. Le Directeur DSI n'a pas accès au workspace projet (tâches, sprints,
 * planning, jalons, équipe, risques) ni à l'Assistant IA — lecture seule sur le portefeuille et
 * les rapports.
 */
export function railItemsForRole(role: Role): RailItem[] {
  switch (role) {
    case Role.ChefProjet:
      return [
        { icone: '▤', libelle: 'Mes projets', route: '/projets', exact: true },
        { icone: '◫', libelle: 'Mon travail', route: '/mon-travail', badgeKey: 'monTravail' },
        { icone: '◴', libelle: 'Pilotage', route: '/pilotage', activePrefixes: ['/projets/'] },
        { icone: '✦', libelle: 'Assistant IA', route: '/assistant-ia', badgeKey: 'ia' },
        { icone: '▦', libelle: 'Rapports', route: '/rapports' },
      ];
    case Role.DirecteurDsi:
      return [
        { icone: '◫', libelle: 'Tableau de bord', route: '/tableau-de-bord' },
        { icone: '▤', libelle: 'Portefeuille', route: '/portefeuille', badgeKey: 'projetsActifs' },
        { icone: '◴', libelle: "Journal d'accès", route: '/administration/journal-acces' },
        { icone: '▦', libelle: 'Rapports', route: '/rapports' },
      ];
    case Role.MembreEquipe:
      return [
        { icone: '▤', libelle: 'Mes projets', route: '/projets' },
        { icone: '◫', libelle: 'Mon travail', route: '/mon-travail', badgeKey: 'monTravail' },
        { icone: '◑', libelle: 'Saisie des temps', route: '/temps' },
        { icone: '▦', libelle: 'Diffusion', route: '/diffusions' },
      ];
    case Role.DirectionMetier:
      return [
        { icone: '◫', libelle: 'Avancement partagé', route: '/espace-client/avancement' },
        { icone: '◇', libelle: 'Étapes clés', route: '/espace-client/etapes' },
        { icone: '▦', libelle: "Points d'information", route: '/espace-client/informations' },
      ];
    case Role.Administrateur:
      return [
        { icone: '◫', libelle: 'Tableau de bord', route: '/administration/tableau-de-bord' },
        { icone: '▤', libelle: 'Compte utilisateur', route: '/administration/comptes' },
        { icone: '◒', libelle: 'Rôle et habilitation', route: '/administration/roles' },
        { icone: '◍', libelle: 'Équipe de projet', route: '/administration/equipe-projet' },
        { icone: '◴', libelle: "Journal d'accès", route: '/administration/journal-acces' },
        { icone: '◇', libelle: 'Partage client', route: '/administration/partage-client' },
      ];
    default:
      return [];
  }
}
