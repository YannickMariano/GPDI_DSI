-- =====================================================================
-- GPDI — Assistant IA global (discussion sur tous les projets du chef de projet,
-- sans choix d'un projet précis : onglet Assistant IA et bulle de discussion).
--
-- À exécuter après init-lot8-risques-ia.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot14-assistant-ia-global.sql
--
-- Un échange global n'est rattaché à aucun projet : id_projet devient facultatif
-- et la portée GLOBAL s'ajoute aux portées existantes. Les analyses existantes
-- (toutes rattachées à un projet) ne sont pas modifiées.
-- Script rejouable sans effet destructeur (IF NOT EXISTS ; DROP NOT NULL sans effet si déjà fait).
-- =====================================================================

ALTER TYPE portee_analyse_ia ADD VALUE IF NOT EXISTS 'GLOBAL';

ALTER TABLE ia_analyses ALTER COLUMN id_projet DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ia_analyses_demandeur_portee ON ia_analyses (id_demandeur, portee, date_creation);
