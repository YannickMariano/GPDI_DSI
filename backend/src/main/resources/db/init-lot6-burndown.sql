-- =====================================================================
-- GPDI — Initialisation Postgres pour le burndown de sprint
-- (instantanes quotidiens du travail restant, captures par une tache planifiee).
--
-- À exécuter après init-lot5-taches.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot6-burndown.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS).
-- =====================================================================

CREATE TABLE IF NOT EXISTS burndown_snapshots (
    id_snapshot      BIGSERIAL PRIMARY KEY,
    id_sprint        BIGINT REFERENCES sprints(id_sprint),
    date             DATE NOT NULL,
    heures_restantes DOUBLE PRECISION NOT NULL
);

DO $$ BEGIN
    ALTER TABLE burndown_snapshots ADD CONSTRAINT uk_burndown_sprint_date UNIQUE (id_sprint, date);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
