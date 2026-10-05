-- =====================================================================
-- GPDI — Initialisation Postgres pour le Lot 2/3 (CRUD Équipes de projet)
--
-- À exécuter après init-lot1-utilisateurs.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot2-equipes.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS).
-- =====================================================================

-- 1. Équipes DSI (entité mg.gov.mef.dsi.gpdi.entity.Equipe)
CREATE TABLE IF NOT EXISTS equipes (
    id_equipe      BIGSERIAL PRIMARY KEY,
    nom            VARCHAR(150) NOT NULL,
    description    VARCHAR(500),
    active         BOOLEAN NOT NULL DEFAULT false,
    date_creation  TIMESTAMP NOT NULL DEFAULT now(),
    id_chef_equipe BIGINT REFERENCES utilisateurs(id)
);

-- 2. Rattachement d'un utilisateur à une équipe (entité MembreEquipe)
CREATE TABLE IF NOT EXISTS membre_equipes (
    id_membre      BIGSERIAL PRIMARY KEY,
    id_equipe      BIGINT NOT NULL REFERENCES equipes(id_equipe),
    id_utilisateur BIGINT NOT NULL REFERENCES utilisateurs(id),
    actif          BOOLEAN NOT NULL DEFAULT true
);
