-- =====================================================================
-- GPDI — Initialisation Postgres pour la matrice d'habilitations (RBAC dynamique)
--
-- A executer apres init-lot1-utilisateurs.sql (necessite la table roles), sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot10-habilitations.sql
--
-- Valeurs initiales identiques a l'ancienne matrice statique du frontend
-- (frontend/src/app/core/services/seed-data.ts, SEED_PERMISSIONS_MATRIX), desormais editable
-- depuis la page Roles & Habilitations et appliquee dynamiquement par le backend.
-- Script rejouable sans effet destructif (ON CONFLICT).
-- =====================================================================

CREATE TABLE IF NOT EXISTS habilitations (
    id        BIGSERIAL PRIMARY KEY,
    module    VARCHAR(50) NOT NULL,
    fonction  VARCHAR(200) NOT NULL,
    role_nom  VARCHAR(50) NOT NULL,
    niveau    VARCHAR(10) NOT NULL,
    CONSTRAINT uk_habilitations_fonction_role UNIQUE (fonction, role_nom)
);

INSERT INTO habilitations (module, fonction, role_nom, niveau) VALUES
    ('Module 0', 'Création et suppression des comptes',      'directeur_dsi',    'AUCUN'),
    ('Module 0', 'Création et suppression des comptes',      'chef_projet',      'AUCUN'),
    ('Module 0', 'Création et suppression des comptes',      'membre_equipe',    'AUCUN'),
    ('Module 0', 'Création et suppression des comptes',      'administrateur',   'ECRITURE'),
    ('Module 0', 'Création et suppression des comptes',      'direction_metier', 'AUCUN'),

    ('Module 0', 'Création et modification des rôles',       'directeur_dsi',    'AUCUN'),
    ('Module 0', 'Création et modification des rôles',       'chef_projet',      'AUCUN'),
    ('Module 0', 'Création et modification des rôles',       'membre_equipe',    'AUCUN'),
    ('Module 0', 'Création et modification des rôles',       'administrateur',   'ECRITURE'),
    ('Module 0', 'Création et modification des rôles',       'direction_metier', 'AUCUN'),

    ('Module 0', 'Définition des habilitations',              'directeur_dsi',    'AUCUN'),
    ('Module 0', 'Définition des habilitations',              'chef_projet',      'AUCUN'),
    ('Module 0', 'Définition des habilitations',              'membre_equipe',    'AUCUN'),
    ('Module 0', 'Définition des habilitations',              'administrateur',   'ECRITURE'),
    ('Module 0', 'Définition des habilitations',              'direction_metier', 'AUCUN'),

    ('Module 0', 'Rattachement aux équipes projets',          'directeur_dsi',    'AUCUN'),
    ('Module 0', 'Rattachement aux équipes projets',          'chef_projet',      'ECRITURE'),
    ('Module 0', 'Rattachement aux équipes projets',          'membre_equipe',    'AUCUN'),
    ('Module 0', 'Rattachement aux équipes projets',          'administrateur',   'ECRITURE'),
    ('Module 0', 'Rattachement aux équipes projets',          'direction_metier', 'AUCUN'),

    ('Module 0', 'Journal des connexions',                    'directeur_dsi',    'LECTURE'),
    ('Module 0', 'Journal des connexions',                    'chef_projet',      'AUCUN'),
    ('Module 0', 'Journal des connexions',                    'membre_equipe',    'AUCUN'),
    ('Module 0', 'Journal des connexions',                    'administrateur',   'ECRITURE'),
    ('Module 0', 'Journal des connexions',                    'direction_metier', 'AUCUN'),

    ('Module 1', 'Création des projets',                      'directeur_dsi',    'LECTURE'),
    ('Module 1', 'Création des projets',                      'chef_projet',      'ECRITURE'),
    ('Module 1', 'Création des projets',                      'membre_equipe',    'AUCUN'),
    ('Module 1', 'Création des projets',                      'administrateur',   'AUCUN'),
    ('Module 1', 'Création des projets',                      'direction_metier', 'AUCUN'),

    ('Module 1', 'Gestion des sprints',                       'directeur_dsi',    'LECTURE'),
    ('Module 1', 'Gestion des sprints',                       'chef_projet',      'ECRITURE'),
    ('Module 1', 'Gestion des sprints',                       'membre_equipe',    'LECTURE'),
    ('Module 1', 'Gestion des sprints',                       'administrateur',   'AUCUN'),
    ('Module 1', 'Gestion des sprints',                       'direction_metier', 'AUCUN'),

    ('Module 1', 'Gestion des jalons et livrables',           'directeur_dsi',    'LECTURE'),
    ('Module 1', 'Gestion des jalons et livrables',           'chef_projet',      'ECRITURE'),
    ('Module 1', 'Gestion des jalons et livrables',           'membre_equipe',    'LECTURE'),
    ('Module 1', 'Gestion des jalons et livrables',           'administrateur',   'AUCUN'),
    ('Module 1', 'Gestion des jalons et livrables',           'direction_metier', 'AUCUN'),

    ('Module 1', 'Tableau de bord et indicateurs',            'directeur_dsi',    'LECTURE'),
    ('Module 1', 'Tableau de bord et indicateurs',            'chef_projet',      'ECRITURE'),
    ('Module 1', 'Tableau de bord et indicateurs',            'membre_equipe',    'LECTURE'),
    ('Module 1', 'Tableau de bord et indicateurs',            'administrateur',   'AUCUN'),
    ('Module 1', 'Tableau de bord et indicateurs',            'direction_metier', 'AUCUN'),

    ('Module 1', 'Clôture du projet',                         'directeur_dsi',    'AUCUN'),
    ('Module 1', 'Clôture du projet',                         'chef_projet',      'ECRITURE'),
    ('Module 1', 'Clôture du projet',                         'membre_equipe',    'AUCUN'),
    ('Module 1', 'Clôture du projet',                         'administrateur',   'AUCUN'),
    ('Module 1', 'Clôture du projet',                         'direction_metier', 'AUCUN'),

    ('Module 2', 'Création des tâches',                       'directeur_dsi',    'AUCUN'),
    ('Module 2', 'Création des tâches',                       'chef_projet',      'ECRITURE'),
    ('Module 2', 'Création des tâches',                       'membre_equipe',    'LECTURE'),
    ('Module 2', 'Création des tâches',                       'administrateur',   'AUCUN'),
    ('Module 2', 'Création des tâches',                       'direction_metier', 'AUCUN'),

    ('Module 2', 'Affectation des tâches',                    'directeur_dsi',    'AUCUN'),
    ('Module 2', 'Affectation des tâches',                    'chef_projet',      'ECRITURE'),
    ('Module 2', 'Affectation des tâches',                    'membre_equipe',    'LECTURE'),
    ('Module 2', 'Affectation des tâches',                    'administrateur',   'AUCUN'),
    ('Module 2', 'Affectation des tâches',                    'direction_metier', 'AUCUN'),

    ('Module 2', 'Priorisation des tâches',                   'directeur_dsi',    'AUCUN'),
    ('Module 2', 'Priorisation des tâches',                   'chef_projet',      'ECRITURE'),
    ('Module 2', 'Priorisation des tâches',                   'membre_equipe',    'LECTURE'),
    ('Module 2', 'Priorisation des tâches',                   'administrateur',   'AUCUN'),
    ('Module 2', 'Priorisation des tâches',                   'direction_metier', 'AUCUN'),

    ('Module 2', 'Changement de statut',                      'directeur_dsi',    'AUCUN'),
    ('Module 2', 'Changement de statut',                      'chef_projet',      'ECRITURE'),
    ('Module 2', 'Changement de statut',                      'membre_equipe',    'ECRITURE'),
    ('Module 2', 'Changement de statut',                      'administrateur',   'AUCUN'),
    ('Module 2', 'Changement de statut',                      'direction_metier', 'AUCUN'),

    ('Module 2', 'Enregistrement du temps passé',             'directeur_dsi',    'LECTURE'),
    ('Module 2', 'Enregistrement du temps passé',             'chef_projet',      'ECRITURE'),
    ('Module 2', 'Enregistrement du temps passé',             'membre_equipe',    'ECRITURE'),
    ('Module 2', 'Enregistrement du temps passé',             'administrateur',   'AUCUN'),
    ('Module 2', 'Enregistrement du temps passé',             'direction_metier', 'AUCUN'),

    ('Module 2', 'Clôture du sprint',                         'directeur_dsi',    'AUCUN'),
    ('Module 2', 'Clôture du sprint',                         'chef_projet',      'ECRITURE'),
    ('Module 2', 'Clôture du sprint',                         'membre_equipe',    'AUCUN'),
    ('Module 2', 'Clôture du sprint',                         'administrateur',   'AUCUN'),
    ('Module 2', 'Clôture du sprint',                         'direction_metier', 'AUCUN'),

    ('Module 1', 'Partage de l''avancement au client',        'directeur_dsi',    'LECTURE'),
    ('Module 1', 'Partage de l''avancement au client',        'chef_projet',      'ECRITURE'),
    ('Module 1', 'Partage de l''avancement au client',        'membre_equipe',    'AUCUN'),
    ('Module 1', 'Partage de l''avancement au client',        'administrateur',   'ECRITURE'),
    ('Module 1', 'Partage de l''avancement au client',        'direction_metier', 'AUCUN'),

    ('Espace client', 'Consultation de l''avancement partagé', 'directeur_dsi',    'LECTURE'),
    ('Espace client', 'Consultation de l''avancement partagé', 'chef_projet',      'LECTURE'),
    ('Espace client', 'Consultation de l''avancement partagé', 'membre_equipe',    'AUCUN'),
    ('Espace client', 'Consultation de l''avancement partagé', 'administrateur',   'AUCUN'),
    ('Espace client', 'Consultation de l''avancement partagé', 'direction_metier', 'LECTURE'),

    ('Module 1', 'Gestion des risques (IA)',                   'directeur_dsi',    'LECTURE'),
    ('Module 1', 'Gestion des risques (IA)',                   'chef_projet',      'ECRITURE'),
    ('Module 1', 'Gestion des risques (IA)',                   'membre_equipe',    'LECTURE'),
    ('Module 1', 'Gestion des risques (IA)',                   'administrateur',   'AUCUN'),
    ('Module 1', 'Gestion des risques (IA)',                   'direction_metier', 'AUCUN'),

    ('Module 1', 'Recommandations de pilotage (IA)',           'directeur_dsi',    'LECTURE'),
    ('Module 1', 'Recommandations de pilotage (IA)',           'chef_projet',      'ECRITURE'),
    ('Module 1', 'Recommandations de pilotage (IA)',           'membre_equipe',    'AUCUN'),
    ('Module 1', 'Recommandations de pilotage (IA)',           'administrateur',   'AUCUN'),
    ('Module 1', 'Recommandations de pilotage (IA)',           'direction_metier', 'AUCUN'),

    ('Module 1', 'Assistant IA (chat projet)',                 'directeur_dsi',    'AUCUN'),
    ('Module 1', 'Assistant IA (chat projet)',                 'chef_projet',      'ECRITURE'),
    ('Module 1', 'Assistant IA (chat projet)',                 'membre_equipe',    'AUCUN'),
    ('Module 1', 'Assistant IA (chat projet)',                 'administrateur',   'AUCUN'),
    ('Module 1', 'Assistant IA (chat projet)',                 'direction_metier', 'AUCUN'),

    ('Espace client', 'Points d''information (client)',        'directeur_dsi',    'AUCUN'),
    ('Espace client', 'Points d''information (client)',        'chef_projet',      'ECRITURE'),
    ('Espace client', 'Points d''information (client)',        'membre_equipe',    'AUCUN'),
    ('Espace client', 'Points d''information (client)',        'administrateur',   'AUCUN'),
    ('Espace client', 'Points d''information (client)',        'direction_metier', 'LECTURE')
ON CONFLICT (fonction, role_nom) DO UPDATE SET niveau = EXCLUDED.niveau, module = EXCLUDED.module;
