-- =====================================================================
-- GPDI — Initialisation Postgres pour les points d'information (Espace client).
--
-- À exécuter après init-lot8-risques-ia.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot9-points-information.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS) : sans risque si la table existe déjà.
-- =====================================================================

CREATE TABLE IF NOT EXISTS points_information (
    id_point_information BIGSERIAL PRIMARY KEY,
    id_projet            BIGINT NOT NULL REFERENCES projets(id_projet),
    id_auteur            BIGINT REFERENCES utilisateurs(id),
    titre                VARCHAR(300),
    corps                TEXT,
    date_creation        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_points_information_projet ON points_information (id_projet, date_creation DESC);
