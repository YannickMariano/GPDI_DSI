-- =====================================================================
-- GPDI — Initialisation Postgres pour le CRUD Tâche
-- (+ sous-tâches, commentaires, dépendances, sous-ressources d'une tâche).
--
-- À exécuter après init-lot4-sprints-jalons.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot5-taches.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS / ADD COLUMN IF NOT EXISTS).
-- =====================================================================

-- 1. Types enumeres Postgres
DO $$ BEGIN
    CREATE TYPE statut_tache AS ENUM ('A_FAIRE','EN_COURS','EN_TEST','BLOQUEE','TERMINEE','REPORTEE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Rattrapage si ce script a deja ete execute avant l'introduction de REPORTEE.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'REPORTEE' AND enumtypid = 'statut_tache'::regtype) THEN
        ALTER TYPE statut_tache ADD VALUE 'REPORTEE';
    END IF;
END $$;

DO $$ BEGIN
    CREATE TYPE type_lien AS ENUM ('DEBUT_DEBUT','DEBUT_FIN','FIN_DEBUT','FIN_FIN');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 2. Taches (entité mg.gov.mef.dsi.gpdi.entity.Tache)
CREATE TABLE IF NOT EXISTS taches (
    id_tache        BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50) UNIQUE,
    titre           VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),
    statut          statut_tache NOT NULL DEFAULT 'A_FAIRE',
    priorite        priorite,
    date_debut      DATE,
    date_fin_prevue DATE,
    date_fin_reelle DATE,
    temps_estime    DOUBLE PRECISION,
    temps_passe     DOUBLE PRECISION,
    demarree_le     TIMESTAMP,
    id_sprint       BIGINT REFERENCES sprints(id_sprint),
    id_projet       BIGINT REFERENCES projets(id_projet)
);

-- Rattrapage si la table existait deja avant l'ajout du chronometre de suivi du temps.
ALTER TABLE taches ADD COLUMN IF NOT EXISTS demarree_le TIMESTAMP;

-- 3. Assignations d'une tache a des membres de l'equipe (entité AffectationTache)
CREATE TABLE IF NOT EXISTS affectation_taches (
    id_affectation   BIGSERIAL PRIMARY KEY,
    id_tache         BIGINT REFERENCES taches(id_tache),
    id_utilisateur   BIGINT REFERENCES utilisateurs(id),
    date_affectation TIMESTAMP NOT NULL DEFAULT now()
);

-- 4. Sous-taches d'une tache (entité SousTache)
CREATE TABLE IF NOT EXISTS sous_taches (
    id_sous_tache BIGSERIAL PRIMARY KEY,
    titre         VARCHAR(500),
    fait          BOOLEAN NOT NULL DEFAULT false,
    id_tache      BIGINT REFERENCES taches(id_tache)
);

-- Rattrapage si la table existait deja avant l'ajout de fait / le renommage description -> titre.
ALTER TABLE sous_taches ADD COLUMN IF NOT EXISTS fait BOOLEAN NOT NULL DEFAULT false;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sous_taches' AND column_name = 'description')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sous_taches' AND column_name = 'titre') THEN
        ALTER TABLE sous_taches RENAME COLUMN description TO titre;
    END IF;
END $$;

-- 5. Commentaires (activite) d'une tache (entité Commentaire)
CREATE TABLE IF NOT EXISTS commentaires (
    id_commentaire BIGSERIAL PRIMARY KEY,
    id_tache       BIGINT REFERENCES taches(id_tache),
    id_auteur      BIGINT REFERENCES utilisateurs(id),
    contenu        VARCHAR(2000),
    date_creation  TIMESTAMP NOT NULL DEFAULT now()
);

-- 6. Dependances entre taches (entité TachePrecedence)
CREATE TABLE IF NOT EXISTS tache_precedences (
    id_tache_source BIGINT NOT NULL REFERENCES taches(id_tache),
    id_tache_cible  BIGINT NOT NULL REFERENCES taches(id_tache),
    type_lien       type_lien NOT NULL,
    PRIMARY KEY (id_tache_source, id_tache_cible)
);
