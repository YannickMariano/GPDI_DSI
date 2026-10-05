-- =====================================================================
-- GPDI — Initialisation Postgres pour la tranche minimale Projets
-- (id/titre/statut) et le CRUD Partage Client.
--
-- À exécuter après init-lot1-utilisateurs.sql et init-lot2-equipes.sql,
-- sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot3-projets-partages.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS / NOT EXISTS).
-- =====================================================================

-- 1. Types enumeres Postgres utilises par les entites Projet et PartageClient
--    (mappes via hibernate-types / PostgreSQLEnumType — pas de simples VARCHAR).
DO $$ BEGIN
    CREATE TYPE statut_projet AS ENUM ('EN_PREPARATION','EN_COURS','SUSPENDU','CLOTURE','ARCHIVE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE priorite AS ENUM ('CRITIQUE','HAUTE','NORMALE','BASSE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE statut_partage AS ENUM ('ACTIF','REVOQUE','EXPIRE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Rattrapage si ce script a deja ete execute avant l'introduction de SUSPENDU (le type existait
-- alors avec DEPASSE a la place) : renomme la valeur sans perte de donnees.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'DEPASSE' AND enumtypid = 'statut_projet'::regtype) THEN
        ALTER TYPE statut_projet RENAME VALUE 'DEPASSE' TO 'SUSPENDU';
    END IF;
END $$;

-- 2. Projets (entité mg.gov.mef.dsi.gpdi.entity.Projet)
CREATE TABLE IF NOT EXISTS projets (
    id_projet      BIGSERIAL PRIMARY KEY,
    titre          VARCHAR(255) NOT NULL,
    code_reference VARCHAR(50),
    description    VARCHAR(1000),
    date_debut     DATE,
    date_fin       DATE,
    date_cloture   DATE,
    statut         statut_projet NOT NULL DEFAULT 'EN_PREPARATION',
    priorite       priorite,
    progression    INTEGER NOT NULL DEFAULT 0,
    archive        BOOLEAN NOT NULL DEFAULT false,
    date_creation  TIMESTAMP NOT NULL DEFAULT now(),
    id_responsable BIGINT REFERENCES utilisateurs(id),
    id_equipe      BIGINT REFERENCES equipes(id_equipe)
);

-- Rattrapage si la table existait deja avant l'ajout de code_reference.
ALTER TABLE projets ADD COLUMN IF NOT EXISTS code_reference VARCHAR(50);

DO $$ BEGIN
    ALTER TABLE projets ADD CONSTRAINT uk_projets_code_reference UNIQUE (code_reference);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3. Partages d'avancement avec un compte Direction métier (entité PartageClient)
CREATE TABLE IF NOT EXISTS partage_clients (
    id_partage     BIGSERIAL PRIMARY KEY,
    id_utilisateur BIGINT NOT NULL REFERENCES utilisateurs(id),
    id_projet      BIGINT NOT NULL REFERENCES projets(id_projet),
    statut         statut_partage NOT NULL DEFAULT 'ACTIF',
    date_creation  TIMESTAMP NOT NULL DEFAULT now(),
    date_echeance  DATE
);

-- 4. Projets de test (repris des personas de démo pour rester reconnaissable),
--    rattachés au chef de projet K. Traoré (matricule 45820, seedé en lot 1).
INSERT INTO projets (titre, code_reference, description, date_debut, date_fin, statut, priorite, progression, id_responsable)
SELECT 'Refonte du portail des marchés', 'GPDI-2026-001',
       'Modernisation du portail de dépôt et de gestion des offres pour les marchés publics.',
       '2026-04-06', '2026-11-28', 'EN_COURS', 'HAUTE', 62, u.id
FROM utilisateurs u WHERE u.matricule = '45820'
AND NOT EXISTS (SELECT 1 FROM projets WHERE titre = 'Refonte du portail des marchés');

INSERT INTO projets (titre, code_reference, description, date_debut, date_fin, statut, priorite, progression, id_responsable)
SELECT 'Interconnexion Trésor Public', 'GPDI-2026-002',
       'Interconnexion des systèmes d''information avec le Trésor Public.',
       '2026-03-02', '2026-10-30', 'EN_COURS', 'CRITIQUE', 37, u.id
FROM utilisateurs u WHERE u.matricule = '45820'
AND NOT EXISTS (SELECT 1 FROM projets WHERE titre = 'Interconnexion Trésor Public');

-- 5. Partage de test : le projet "Refonte du portail des marchés" avec le compte
--    Direction métier L. Sawadogo (matricule 50021, seedé en lot 1).
INSERT INTO partage_clients (id_utilisateur, id_projet, statut, date_echeance)
SELECT client.id, projet.id_projet, 'ACTIF', '2026-12-31'
FROM utilisateurs client, projets projet
WHERE client.matricule = '50021' AND projet.titre = 'Refonte du portail des marchés'
AND NOT EXISTS (
    SELECT 1 FROM partage_clients pc
    WHERE pc.id_utilisateur = client.id AND pc.id_projet = projet.id_projet
);
