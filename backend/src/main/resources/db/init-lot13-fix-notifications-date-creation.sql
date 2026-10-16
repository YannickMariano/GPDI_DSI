-- =====================================================================
-- GPDI — Correctif : la colonne notifications.date_creation a ete creee en
-- type "date" sur certaines bases (derive par rapport au script
-- init-lot7-notifications.sql, qui declare bien "TIMESTAMP"). Le type
-- "date" tronque systematiquement l'heure a minuit : toutes les
-- notifications affichaient donc un horodatage ("il y a X heures") faux,
-- calcule depuis minuit au lieu de l'heure reelle de creation.
--
-- À exécuter sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot13-fix-notifications-date-creation.sql
--
-- Script rejouable sans effet destructeur : ne change rien si la colonne
-- est deja du bon type. L'heure des notifications deja enregistrees (avant
-- ce correctif) reste perdue (jamais stockee), mais celles creees a partir
-- de maintenant auront leur horodatage exact.
-- =====================================================================

ALTER TABLE notifications
    ALTER COLUMN date_creation TYPE TIMESTAMP USING date_creation::timestamp,
    ALTER COLUMN date_creation SET DEFAULT now();
