-- =====================================================================
-- GPDI — Initialisation Postgres pour la gestion des risques assistée par l'IA
-- (traces d'analyses IA, solutions proposées, risques d'un projet).
--
-- À exécuter après init-lot7-notifications.sql, sur la base "GPDI" :
--   psql -h localhost -U postgres -d GPDI -f init-lot8-risques-ia.sql
--
-- Script rejouable sans effet destructeur (IF NOT EXISTS) : sans risque si les tables existent déjà.
-- =====================================================================

-- 1. Types énumérés Postgres
DO $$ BEGIN
    CREATE TYPE type_analyse_ia AS ENUM ('ANALYSE_PROJET','PLANIFICATION_SPRINT','CHARGE_EQUIPE','DETECTION_RISQUE','SUGGESTION_RISQUE','SYNTHESE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE portee_analyse_ia AS ENUM ('PROJET','SPRINT','EQUIPE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE statut_recommandation AS ENUM ('PROPOSEE','APPLIQUEE','REJETEE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE niveau_risque AS ENUM ('FAIBLE','MOYEN','ELEVE','CRITIQUE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE statut_risque AS ENUM ('OUVERT','SOUS_CONTROLE','REJETE','CLOS');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 2. Trace d'un appel à l'assistant IA (entité IaAnalyse) — générique, réutilisable par d'autres modules IA
CREATE TABLE IF NOT EXISTS ia_analyses (
    id_analyse      BIGSERIAL PRIMARY KEY,
    id_projet       BIGINT NOT NULL REFERENCES projets(id_projet),
    id_demandeur    BIGINT REFERENCES utilisateurs(id),
    type_analyse    type_analyse_ia NOT NULL,
    portee          portee_analyse_ia NOT NULL DEFAULT 'PROJET',
    question        TEXT,
    reponse         TEXT,
    contexte        JSONB,
    indice_confiance NUMERIC(4,3),
    modele          VARCHAR(255),
    date_creation   TIMESTAMP NOT NULL DEFAULT now()
);

-- 3. Risques d'un projet (entité Risque)
CREATE TABLE IF NOT EXISTS risques (
    id_risque         BIGSERIAL PRIMARY KEY,
    id_projet         BIGINT NOT NULL REFERENCES projets(id_projet),
    id_analyse        BIGINT REFERENCES ia_analyses(id_analyse),
    id_porteur        BIGINT REFERENCES utilisateurs(id),
    libelle           VARCHAR(300),
    description       TEXT,
    probabilite       INTEGER,
    impact            INTEGER,
    niveau            niveau_risque,
    statut            statut_risque NOT NULL DEFAULT 'OUVERT',
    mesure_mitigation TEXT,
    date_creation     TIMESTAMP NOT NULL DEFAULT now(),
    date_mise_a_jour  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_risques_projet ON risques (id_projet, id_risque);

-- 4. Solutions proposées par l'IA, à valider ou rejeter (entité IaRecommandation)
CREATE TABLE IF NOT EXISTS ia_recommandations (
    id_recommandation    BIGSERIAL PRIMARY KEY,
    id_analyse           BIGINT NOT NULL REFERENCES ia_analyses(id_analyse),
    id_risque            BIGINT REFERENCES risques(id_risque),
    libelle              VARCHAR(300),
    justification        TEXT,
    action_proposee      JSONB,
    statut               statut_recommandation NOT NULL DEFAULT 'PROPOSEE',
    id_validateur        BIGINT REFERENCES utilisateurs(id),
    motif_rejet          TEXT,
    date_validation      TIMESTAMP,
    resultat_application JSONB,
    date_creation        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ia_recommandations_risque ON ia_recommandations (id_risque, statut);
