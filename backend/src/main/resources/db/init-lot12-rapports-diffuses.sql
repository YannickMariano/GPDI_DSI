-- =====================================================================
-- GPDI — Initialisation Postgres pour les rapports diffusés par les chefs de projet.
--
-- À exécuter après les scripts précédents, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot12-rapports-diffuses.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS) : sans risque si la table existe déjà.
-- Le contenu est une photo figée (JSON) du rapport tel qu'il était au moment de la diffusion.
-- =====================================================================

CREATE TABLE IF NOT EXISTS rapports_diffuses (
    id_rapport_diffuse BIGSERIAL PRIMARY KEY,
    id_projet          BIGINT NOT NULL REFERENCES projets(id_projet),
    id_auteur          BIGINT REFERENCES utilisateurs(id),
    modele             VARCHAR(50) NOT NULL,
    nom                VARCHAR(200) NOT NULL,
    titre              VARCHAR(300),
    contenu            TEXT NOT NULL,
    date_diffusion     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_rapports_diffuses_projet ON rapports_diffuses (id_projet, date_diffusion DESC);
