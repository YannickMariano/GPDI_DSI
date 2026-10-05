-- =====================================================================
-- GPDI — Initialisation Postgres pour le CRUD Sprint et le CRUD Jalon
-- (+ Livrables, sous-ressource d'un jalon).
--
-- À exécuter après init-lot3-projets-partages.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot4-sprints-jalons.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS / ADD COLUMN IF NOT EXISTS).
-- =====================================================================

-- 1. Types enumeres Postgres
DO $$ BEGIN
    CREATE TYPE statut_sprint AS ENUM ('PLANIFIE','EN_COURS','CLOTURER');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE statut_jalon AS ENUM ('A_VENIR','EN_RETARD','ATTEINT','ANNULER');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE statut_livrable AS ENUM ('ATTENDU','LIVRE','REJETE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 2. Sprints (entité mg.gov.mef.dsi.gpdi.entity.Sprint)
CREATE TABLE IF NOT EXISTS sprints (
    id_sprint       BIGSERIAL PRIMARY KEY,
    nom             VARCHAR(150) NOT NULL,
    date_debut      DATE,
    date_fin        DATE,
    objectif        VARCHAR(1000),
    capacite_heures INTEGER NOT NULL DEFAULT 40,
    progression     INTEGER NOT NULL DEFAULT 0,
    statut          statut_sprint NOT NULL DEFAULT 'PLANIFIE',
    date_creation   TIMESTAMP NOT NULL DEFAULT now(),
    id_projet       BIGINT REFERENCES projets(id_projet)
);

-- Rattrapage si la table existait deja avant l'ajout de capacite_heures.
ALTER TABLE sprints ADD COLUMN IF NOT EXISTS capacite_heures INTEGER NOT NULL DEFAULT 40;

-- 3. Jalons (entité mg.gov.mef.dsi.gpdi.entity.Jalon)
CREATE TABLE IF NOT EXISTS jalons (
    id_jalon        BIGSERIAL PRIMARY KEY,
    titre           VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    date_echeance   DATE,
    statut          statut_jalon NOT NULL DEFAULT 'A_VENIR',
    date_validation DATE,
    id_projet       BIGINT REFERENCES projets(id_projet)
);

-- 4. Livrables d'un jalon (entité mg.gov.mef.dsi.gpdi.entity.Livrable)
CREATE TABLE IF NOT EXISTS livrables (
    id_livrable     BIGSERIAL PRIMARY KEY,
    description     VARCHAR(1000),
    type            VARCHAR(100),
    statut          statut_livrable NOT NULL DEFAULT 'ATTENDU',
    date_livraison  DATE,
    date_validation DATE,
    date_rejet      DATE,
    motif_rejet     VARCHAR(500),
    id_jalon        BIGINT REFERENCES jalons(id_jalon)
);
