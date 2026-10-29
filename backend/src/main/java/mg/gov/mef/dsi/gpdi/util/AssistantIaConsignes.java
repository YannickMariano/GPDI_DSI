package mg.gov.mef.dsi.gpdi.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consignes envoyées au modèle pour répondre à une question libre d'un chef de projet sur son projet,
 * et schéma JSON imposé à sa réponse. Les consignes sont rédigées avec leurs accents : un modèle
 * recopie l'orthographe de son prompt, et ses réponses sont lues telles quelles par les utilisateurs.
 */
public final class AssistantIaConsignes {

    private static final String CONSIGNE_SYSTEME =
            "Tu es l'assistant IA du chef de projet, pour le pilotage d'un projet informatique d'une administration. "
                    + "On te donne les données JSON du projet (avancement, équipe et charge, sprints, jalons, tâches à "
                    + "surveiller, risques déjà connus) et une question du chef de projet à ce sujet.\n"
                    + "\n"
                    + "Règles :\n"
                    + "- Réponds uniquement à partir des données fournies. N'invente aucun fait, chiffre, nom ou identifiant.\n"
                    + "- Si les données ne permettent pas de répondre précisément, dis-le clairement plutôt que de deviner.\n"
                    + "- Réponds uniquement par un objet JSON conforme au schéma demandé. Rédige en français correct, avec tous les accents.\n"
                    + "- reponse : la réponse à la question, claire et directement utile pour un chef de projet, en quelques phrases. "
                    + "Cite les codes de tâche, noms de personnes ou dates utiles, jamais un identifiant numérique brut.\n"
                    + "- fondement (optionnel) : en quelques mots, sur quelles données de la réponse tu t'appuies (par exemple "
                    + "« 3 sprints clôturés, 12 tâches ouvertes »). Omets ce champ si la réponse est directe et n'en a pas besoin.";

    private static final String CONSIGNE_SYSTEME_PORTEFEUILLE =
            "Tu es l'assistant IA du chef de projet, pour le pilotage de l'ensemble de ses projets informatiques dans une "
                    + "administration. On te donne les données JSON de tous ses projets (pour chacun : fiche du projet, équipe et "
                    + "charge, tâches à surveiller, sprints non clôturés, jalons ouverts, risques actifs ; les projets clôturés ou "
                    + "archivés ne sont décrits que par leur fiche) et une question du chef de projet.\n"
                    + "\n"
                    + "Règles :\n"
                    + "- Réponds uniquement à partir des données fournies. N'invente aucun fait, chiffre, nom ou identifiant.\n"
                    + "- Si la question porte sur un projet précis, réponds sur ce projet ; sinon, réponds pour l'ensemble des "
                    + "projets en comparant ou en regroupant selon ce qui est utile. Nomme toujours le projet concerné par son titre.\n"
                    + "- Si les données ne permettent pas de répondre précisément, dis-le clairement plutôt que de deviner.\n"
                    + "- Réponds uniquement par un objet JSON conforme au schéma demandé. Rédige en français correct, avec tous les accents.\n"
                    + "- reponse : la réponse à la question, claire et directement utile pour un chef de projet, en quelques phrases. "
                    + "Cite les titres de projet, codes de tâche, noms de personnes ou dates utiles, jamais un identifiant numérique brut.\n"
                    + "- fondement (optionnel) : en quelques mots, sur quelles données de la réponse tu t'appuies (par exemple "
                    + "« 3 projets en cours, 5 tâches en retard »). Omets ce champ si la réponse est directe et n'en a pas besoin.";

    private AssistantIaConsignes() {
    }

    public static String consigneSysteme() {
        return CONSIGNE_SYSTEME;
    }

    public static String consigneUtilisateur(JsonNode contexteProjet, String question) {
        return "Données du projet (JSON) :\n" + contexteProjet.toString()
                + "\n\nQuestion du chef de projet :\n" + question;
    }

    public static String consigneSystemePortefeuille() {
        return CONSIGNE_SYSTEME_PORTEFEUILLE;
    }

    public static String consigneUtilisateurPortefeuille(JsonNode contextePortefeuille, String question) {
        return "Données de tous les projets du chef de projet (JSON) :\n" + contextePortefeuille.toString()
                + "\n\nQuestion du chef de projet :\n" + question;
    }

    public static Map<String, Object> schemaReponse() {
        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("type", "string");

        Map<String, Object> fondement = new LinkedHashMap<>();
        fondement.put("type", "string");

        Map<String, Object> proprietes = new LinkedHashMap<>();
        proprietes.put("reponse", reponse);
        proprietes.put("fondement", fondement);

        Map<String, Object> racine = new LinkedHashMap<>();
        racine.put("type", "object");
        racine.put("properties", proprietes);
        racine.put("required", Arrays.asList("reponse"));
        return racine;
    }
}
