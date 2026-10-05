-- =====================================================================
-- GPDI — Initialisation Postgres pour les pieces jointes de commentaires de tache.
--
-- A executer apres init-lot5-taches.sql (necessite la table commentaires), sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot11-pieces-jointes-commentaires.sql
--
-- Une seule piece jointe par commentaire (contrainte UNIQUE sur id_commentaire). Le contenu est
-- stocke directement en base (BYTEA) : pas de stockage fichier externe a provisionner.
-- Script rejouable sans effet destructeur (IF NOT EXISTS).
-- =====================================================================

CREATE TABLE IF NOT EXISTS pieces_jointes_commentaires (
    id_piece_jointe BIGSERIAL PRIMARY KEY,
    id_commentaire  BIGINT NOT NULL UNIQUE REFERENCES commentaires(id_commentaire),
    nom_fichier     VARCHAR(255) NOT NULL,
    type_contenu    VARCHAR(100) NOT NULL,
    taille          BIGINT NOT NULL,
    contenu         BYTEA NOT NULL,
    date_creation   TIMESTAMP NOT NULL DEFAULT now()
);
