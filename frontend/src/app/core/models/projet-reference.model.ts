/**
 * Vue minimale d'un projet (entité backend `Projet`, `/api/projets`), utilisée là où seule
 * l'identité du projet est nécessaire (ex. sélecteur de projet pour un partage client).
 * Ne pas confondre avec `Project` (core/models/project.model.ts), le modèle riche encore
 * mocké (avancement, santé, sprints…) consommé par le module Projets (Kanban, Portfolio…).
 */
export interface ProjetReference {
  id: string;
  titre: string;
  statut: string;
}
