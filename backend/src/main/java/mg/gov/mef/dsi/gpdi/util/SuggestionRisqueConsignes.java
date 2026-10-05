package mg.gov.mef.dsi.gpdi.util;

import com.fasterxml.jackson.databind.JsonNode;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consignes envoyées au modèle pour proposer des solutions à un risque, et schéma JSON imposé à sa
 * réponse. Le schéma est volontairement à plat (une solution = un objet sans imbrication) : les petits
 * modèles respectent mieux un schéma simple. Les consignes sont rédigées avec leurs accents : un
 * modèle recopie l'orthographe de son prompt, et ses réponses sont lues telles quelles par les utilisateurs.
 */
public final class SuggestionRisqueConsignes {

    /** Plafond de solutions par demande, annoncé au modèle et réappliqué à sa réponse. */
    public static final int MAX_SOLUTIONS = 4;

    private static final String CONSIGNE_SYSTEME =
            "Tu es un assistant expert en pilotage de projets informatiques pour une administration. "
                    + "On te donne les données JSON d'un projet et l'un de ses risques. "
                    + "Tu proposes des solutions concrètes, exécutables sur le projet, pour réduire ce risque.\n"
                    + "\n"
                    + "Règles :\n"
                    + "- Base-toi uniquement sur les données fournies. N'invente aucun fait, chiffre, nom ou identifiant.\n"
                    + "- Réponds uniquement par un objet JSON conforme au schéma demandé. Rédige en français correct, avec tous les accents.\n"
                    + "- Propose de 2 à " + MAX_SOLUTIONS + " solutions différentes, de la plus efficace à la moins efficace.\n"
                    + "- typeAction vaut soit " + TypeActionRecommandation.REASSIGNER_TACHE + " (confier une tâche existante à un autre membre), "
                    + "soit " + TypeActionRecommandation.CREER_TACHE + " (créer une nouvelle tâche : mesure de mitigation, de suivi ou de replanification).\n"
                    + "- " + TypeActionRecommandation.REASSIGNER_TACHE + " : idTache est le champ \"id\" d'une tâche de \"taches.aSurveiller\" ; "
                    + "idAssigne est le champ \"id\" d'un membre de \"equipe\", de préférence peu chargé (tachesOuvertes, heuresRestantes). "
                    + "Ne réaffecte pas une tâche à la personne qui la détient déjà. Si aucune réaffectation n'est justifiée, ne la force pas.\n"
                    + "- " + TypeActionRecommandation.CREER_TACHE + " : titreTache est un titre court à l'infinitif, descriptionTache détaille la tâche, "
                    + "tempsEstime est sa durée en heures, idAssigne (optionnel) est le champ \"id\" d'un membre de \"equipe\". Omets idAssigne si tu hésites.\n"
                    + "- libelle : la solution en une phrase claire pour un chef de projet. Cite le code de la tâche et le nom du membre, jamais un identifiant numérique.\n"
                    + "- justification : une ou deux phrases expliquant en quoi la solution réduit le risque.\n"
                    + "- gain : le bénéfice attendu en une phrase courte (par exemple un retard évité). "
                    + "N'avance aucun chiffre que les données ne permettent pas de déduire ; sinon formule-le qualitativement.";

    private SuggestionRisqueConsignes() {
    }

    public static String consigneSysteme() {
        return CONSIGNE_SYSTEME;
    }

    public static String consigneUtilisateur(JsonNode contexteProjet, JsonNode risque) {
        return "Données du projet (JSON) :\n" + contexteProjet.toString()
                + "\n\nRisque à traiter (JSON) :\n" + risque.toString()
                + "\n\nPropose des solutions pour réduire ce risque.";
    }

    public static Map<String, Object> schemaReponse() {
        Map<String, Object> typeAction = new LinkedHashMap<>();
        typeAction.put("type", "string");
        typeAction.put("enum", Arrays.asList(TypeActionRecommandation.CREER_TACHE.name(),
                TypeActionRecommandation.REASSIGNER_TACHE.name()));

        Map<String, Object> solution = new LinkedHashMap<>();
        solution.put("libelle", type("string"));
        solution.put("justification", type("string"));
        solution.put("gain", type("string"));
        solution.put("typeAction", typeAction);
        solution.put("titreTache", type("string"));
        solution.put("descriptionTache", type("string"));
        solution.put("tempsEstime", type("number"));
        solution.put("idTache", type("integer"));
        solution.put("idAssigne", type("integer"));

        Map<String, Object> solutionSchema = new LinkedHashMap<>();
        solutionSchema.put("type", "object");
        solutionSchema.put("properties", solution);
        solutionSchema.put("required", Arrays.asList("libelle", "justification", "gain", "typeAction"));

        Map<String, Object> solutions = new LinkedHashMap<>();
        solutions.put("type", "array");
        solutions.put("items", solutionSchema);

        Map<String, Object> proprietes = new LinkedHashMap<>();
        proprietes.put("solutions", solutions);

        Map<String, Object> racine = new LinkedHashMap<>();
        racine.put("type", "object");
        racine.put("properties", proprietes);
        racine.put("required", Arrays.asList("solutions"));
        return racine;
    }

    private static Map<String, Object> type(String type) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        return schema;
    }
}
