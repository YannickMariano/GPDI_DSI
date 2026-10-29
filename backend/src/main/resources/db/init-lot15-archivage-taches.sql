-- =====================================================================
-- GPDI — Archivage des tâches terminées (onglet Kanban : bouton « Archiver »,
-- vue « Archives » listant les tâches archivées du projet).
--
-- À exécuter après init-lot5-taches.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot15-archivage-taches.sql
--
-- Une tâche archivée reste en base avec toutes ses données (statut TERMINEE, sprint,
-- temps, assignations) : seul l'affichage dans le Kanban change.
-- Script rejouable sans effet destructeur (ADD COLUMN IF NOT EXISTS).
-- =====================================================================

ALTER TABLE taches ADD COLUMN IF NOT EXISTS archivee BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE taches ADD COLUMN IF NOT EXISTS date_archivage TIMESTAMP;
