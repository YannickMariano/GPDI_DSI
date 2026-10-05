-- =====================================================================
-- GPDI — Initialisation Postgres pour les notifications personnelles
-- (lu / non lu par utilisateur, generees depuis les evenements du backend).
--
-- À exécuter après init-lot6-burndown.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot7-notifications.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS).
-- =====================================================================

CREATE TABLE IF NOT EXISTS notifications (
    id_notification BIGSERIAL PRIMARY KEY,
    id_utilisateur  BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,
    titre           VARCHAR(300) NOT NULL,
    meta            VARCHAR(300),
    lien            VARCHAR(300),
    lu              BOOLEAN NOT NULL DEFAULT false,
    cle             VARCHAR(150),
    date_creation   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_destinataire ON notifications (id_utilisateur, date_creation DESC);

-- Un meme destinataire ne recoit jamais deux fois la meme notification generee par une tache planifiee.
CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_destinataire_cle
    ON notifications (id_utilisateur, cle) WHERE cle IS NOT NULL;
