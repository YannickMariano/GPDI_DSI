-- =====================================================================
-- GPDI — Actions proposées par l'assistant IA (créer, modifier, affecter, changer de
-- statut, démarrer, clôturer, archiver, restaurer, supprimer…), exécutées uniquement
-- après confirmation explicite de l'utilisateur.
--
-- À exécuter après init-lot14-assistant-ia-global.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot16-actions-assistant-ia.sql
--
-- type_action et statut en VARCHAR (comme la table habilitations) : table de suivi peu
-- volumineuse, sans besoin de type ENUM natif.
-- Script rejouable sans effet destructeur (IF NOT EXISTS).
-- =====================================================================

CREATE TABLE IF NOT EXISTS ia_actions_assistant (
    id_action        BIGSERIAL PRIMARY KEY,
    id_analyse       BIGINT NOT NULL REFERENCES ia_analyses(id_analyse) ON DELETE CASCADE,
    id_demandeur     BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    type_action      VARCHAR(40) NOT NULL,
    libelle          VARCHAR(300) NOT NULL,
    parametres       JSONB NOT NULL,
    statut           VARCHAR(20) NOT NULL DEFAULT 'PROPOSEE',
    resultat         VARCHAR(500),
    date_creation    TIMESTAMP NOT NULL DEFAULT now(),
    date_traitement  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ia_actions_assistant_analyse ON ia_actions_assistant (id_analyse);
