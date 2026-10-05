-- =====================================================================
-- GPDI — Initialisation Postgres pour le Lot 1 (authentification Keycloak)
-- et préparation du Lot 2 (utilisateurs / rôles).
--
-- À exécuter manuellement sur la base "GPDI" (psql, pgAdmin, DBeaver...) :
--   psql -h localhost -U postgres -d GPDI -f init-lot1-utilisateurs.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS / ON CONFLICT).
-- spring.jpa.hibernate.ddl-auto=none : le schéma n'est pas généré par
-- Hibernate, il doit exister avant le démarrage de l'application.
-- =====================================================================

-- 1. Rôles applicatifs (entité mg.gov.mef.dsi.gpdi.entity.Role)
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 2. Utilisateurs applicatifs (entité mg.gov.mef.dsi.gpdi.entity.Utilisateur)
CREATE TABLE IF NOT EXISTS utilisateurs (
    id            BIGSERIAL PRIMARY KEY,
    matricule     VARCHAR(50)  NOT NULL UNIQUE,
    nom           VARCHAR(100) NOT NULL,
    prenom        VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    telephone     VARCHAR(20),
    mot_de_passe  VARCHAR(255),
    specialite    VARCHAR(150),
    actif         BOOLEAN NOT NULL DEFAULT false,
    date_creation TIMESTAMP NOT NULL DEFAULT now(),
    id_role       BIGINT REFERENCES roles(id)
);

-- 3. Les 5 rôles système attendus par le frontend (core/models/role.model.ts).
--    Le libellé "nom" doit rester EXACTEMENT ces valeurs : AuthService en
--    dépend pour reconnaître le rôle après connexion.
INSERT INTO roles (nom, description) VALUES
  ('administrateur',   'Administre les comptes, les rôles, les habilitations, les rattachements et les référentiels.'),
  ('directeur_dsi',    'Consulte le portefeuille, les indicateurs et le journal des connexions. Aucun droit de modification opérationnelle.'),
  ('chef_projet',      'Crée les projets, planifie les sprints et les jalons, crée et affecte les tâches, partage l''avancement au client.'),
  ('membre_equipe',    'Fait évoluer le statut de ses tâches, enregistre son temps, consulte les dépendances.'),
  ('direction_metier', 'Consulte uniquement l''état d''avancement des projets qui lui sont explicitement partagés.')
ON CONFLICT (nom) DO UPDATE SET description = EXCLUDED.description;

-- 4. Comptes de test — un par rôle système, repris des personas de démo
--    du frontend (seed-data.ts) pour rester reconnaissable pendant les tests.
--    Le mot de passe stocké ici N'EST PAS utilisé pour l'authentification
--    (celle-ci passe entièrement par Keycloak) : il ne sert qu'à satisfaire
--    la colonne. Pensez à créer, pour CHAQUE matricule ci-dessous, un
--    utilisateur Keycloak avec ce même "username" et un mot de passe réel.
INSERT INTO utilisateurs (matricule, nom, prenom, email, mot_de_passe, specialite, actif, id_role)
SELECT '10001', 'Nikièma', 'D', 'd.nikiema@mef.gov', 'NON_UTILISE_AUTH_KEYCLOAK', 'DSI', true, r.id
FROM roles r WHERE r.nom = 'administrateur'
ON CONFLICT (matricule) DO NOTHING;

INSERT INTO utilisateurs (matricule, nom, prenom, email, mot_de_passe, specialite, actif, id_role)
SELECT '10002', 'Compaoré', 'S', 's.compaore@mef.gov', 'NON_UTILISE_AUTH_KEYCLOAK', 'DSI', true, r.id
FROM roles r WHERE r.nom = 'directeur_dsi'
ON CONFLICT (matricule) DO NOTHING;

INSERT INTO utilisateurs (matricule, nom, prenom, email, mot_de_passe, specialite, actif, id_role)
SELECT '45820', 'Traoré', 'K', 'k.traore@mef.gov', 'NON_UTILISE_AUTH_KEYCLOAK', 'Direction des Marchés Publics', true, r.id
FROM roles r WHERE r.nom = 'chef_projet'
ON CONFLICT (matricule) DO NOTHING;

INSERT INTO utilisateurs (matricule, nom, prenom, email, mot_de_passe, specialite, actif, id_role)
SELECT '33110', 'Koffi', 'A', 'a.koffi@mef.gov', 'NON_UTILISE_AUTH_KEYCLOAK', 'Direction des Marchés Publics', true, r.id
FROM roles r WHERE r.nom = 'membre_equipe'
ON CONFLICT (matricule) DO NOTHING;

INSERT INTO utilisateurs (matricule, nom, prenom, email, mot_de_passe, specialite, actif, id_role)
SELECT '50021', 'Sawadogo', 'L', 'l.sawadogo@mef.gov', 'NON_UTILISE_AUTH_KEYCLOAK', 'Direction des Marchés Publics', true, r.id
FROM roles r WHERE r.nom = 'direction_metier'
ON CONFLICT (matricule) DO NOTHING;
