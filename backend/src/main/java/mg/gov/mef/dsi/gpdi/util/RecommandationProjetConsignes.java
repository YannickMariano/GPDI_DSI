package mg.gov.mef.dsi.gpdi.util;

import com.fasterxml.jackson.databind.JsonNode;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandationProjet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consignes envoyées au modèle pour proposer des actions de pilotage sur un projet (réaffectation,
 * désengorgement de sprint, réestimation), et schéma JSON imposé à sa réponse.
 */
public final class RecommandationProjetConsignes {

    /** Plafond de recommandations par demande, annoncé au modèle et réappliqué à sa réponse. */
    public static final int MAX_RECOMMANDATIONS = 6;

    private static final String CONSIGNE_SYSTEME =
            "Tu es l'assistant IA du chef de projet, pour le pilotage d'un projet informatique d'une administration. "
                    + "On te donne les données JSON du projet (équipe et charge, sprints avec leur capacité, tâches à surveiller). "
                    + "Tu identifies des actions concrètes et justifiées pour améliorer le pilotage du projet.\n"
                    + "\n"
                    + "Règles :\n"
                    + "- Base-toi uniquement sur les données fournies. N'invente aucun fait, chiffre, nom ou identifiant.\n"
                    + "- Réponds uniquement par un objet JSON conforme au schéma demandé. Rédige en français correct, avec tous les accents.\n"
                    + "- Propose de 0 à " + MAX_RECOMMANDATIONS + " recommandations, seulement si elles sont réellement justifiées : "
                    + "ne force jamais une recommandation si rien ne la justifie clairement dans les données.\n"
                    + "- Ne répète pas une recommandation déjà proposée (voir \"recommandationsDejaProposees\" dans les données), même reformulée.\n"
                    + "- type vaut " + TypeActionRecommandationProjet.REASSIGNER_TACHE + ", " + TypeActionRecommandationProjet.RETIRER_DU_SPRINT
                    + " ou " + TypeActionRecommandationProjet.AJUSTER_ESTIMATION + ".\n"
                    + "- " + TypeActionRecommandationProjet.REASSIGNER_TACHE + " : idTache est le champ \"id\" d'une tâche de \"taches.aSurveiller\" "
                    + "assignée à un membre en surcharge (tachesOuvertes, heuresRestantes élevés) ; idAssigne est le champ \"id\" d'un membre de "
                    + "\"equipe\" nettement moins chargé. Ne réaffecte pas une tâche à la personne qui la détient déjà.\n"
                    + "- " + TypeActionRecommandationProjet.RETIRER_DU_SPRINT + " : idTache est le champ \"id\" d'une tâche de \"taches.aSurveiller\" "
                    + "dont \"idSprint\" correspond à un sprint de \"sprints\" en surcapacité (heuresRestantes du sprint nettement supérieur à sa "
                    + "capaciteHeures).\n"
                    + "- " + TypeActionRecommandationProjet.AJUSTER_ESTIMATION + " : idTache est le champ \"id\" d'une tâche de \"taches.aSurveiller\" "
                    + "dont l'estimation (heuresEstimees) semble irréaliste au vu des données ; nouvelleEstimation est la nouvelle estimation en "
                    + "heures, strictement positive.\n"
                    + "- libelle : la recommandation en une phrase claire pour un chef de projet. Cite le code de la tâche et les noms de personnes, "
                    + "jamais un identifiant numérique.\n"
                    + "- justification : une ou deux phrases expliquant en quoi cette action améliore le pilotage du projet.\n"
                    + "- preuve : en quelques mots, la donnée précise sur laquelle elle s'appuie (par exemple « Sprint 4 : 41 h engagées pour 35 h "
                    + "de capacité »).";

    private RecommandationProjetConsignes() {
    }

    public static String consigneSysteme() {
        return CONSIGNE_SYSTEME;
    }

    public static String consigneUtilisateur(JsonNode contexteProjet, List<String> recommandationsDejaProposees) {
        List<String> libelles = recommandationsDejaProposees == null ? new ArrayList<>() : recommandationsDejaProposees;
        return "Données du projet (JSON) :\n" + contexteProjet.toString()
                + "\n\nRecommandations déjà proposées (à ne pas répéter, même reformulées) : "
                + (libelles.isEmpty() ? "aucune" : String.join(" | ", libelles))
                + "\n\nPropose des actions de pilotage pour ce projet, seulement si elles sont justifiées.";
    }

    public static Map<String, Object> schemaReponse() {
        Map<String, Object> type = new LinkedHashMap<>();
        type.put("type", "string");
        type.put("enum", Arrays.asList(TypeActionRecommandationProjet.REASSIGNER_TACHE.name(),
                TypeActionRecommandationProjet.RETIRER_DU_SPRINT.name(), TypeActionRecommandationProjet.AJUSTER_ESTIMATION.name()));

        Map<String, Object> recommandation = new LinkedHashMap<>();
        recommandation.put("type", type);
        recommandation.put("libelle", champ("string"));
        recommandation.put("justification", champ("string"));
        recommandation.put("preuve", champ("string"));
        recommandation.put("idTache", champ("integer"));
        recommandation.put("idAssigne", champ("integer"));
        recommandation.put("nouvelleEstimation", champ("number"));

        Map<String, Object> recommandationSchema = new LinkedHashMap<>();
        recommandationSchema.put("type", "object");
        recommandationSchema.put("properties", recommandation);
        recommandationSchema.put("required", Arrays.asList("type", "libelle", "justification", "idTache"));

        Map<String, Object> recommandations = new LinkedHashMap<>();
        recommandations.put("type", "array");
        recommandations.put("items", recommandationSchema);

        Map<String, Object> proprietes = new LinkedHashMap<>();
        proprietes.put("recommandations", recommandations);

        Map<String, Object> racine = new LinkedHashMap<>();
        racine.put("type", "object");
        racine.put("properties", proprietes);
        racine.put("required", Arrays.asList("recommandations"));
        return racine;
    }

    private static Map<String, Object> champ(String type) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        return schema;
    }
}
