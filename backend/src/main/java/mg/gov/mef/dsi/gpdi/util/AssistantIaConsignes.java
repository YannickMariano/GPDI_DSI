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
                    + "« 3 projets en cours, 5 tâches en retard »). Omets ce champ si la réponse est directe et n'en a pas besoin.\n"
                    + "\n"
                    + "Actions :\n"
                    + "- Si le chef de projet te demande de FAIRE quelque chose (créer, modifier, affecter, retirer, changer de "
                    + "statut, démarrer, suspendre, reprendre, clôturer, marquer un jalon atteint, annuler, archiver, restaurer, "
                    + "supprimer), remplis le champ action avec UNE seule action. Tu n'exécutes rien toi-même : l'action ne sera "
                    + "faite qu'après sa confirmation. Rédige alors reponse comme une proposition courte, par exemple « Je vous "
                    + "propose de créer la tâche … Confirmez ci-dessous. » N'écris jamais que c'est fait.\n"
                    + "- Pour une simple question, n'ajoute pas de champ action.\n"
                    + "- Si plusieurs actions sont demandées, propose la première et dis qu'il faudra redemander les suivantes.\n"
                    + "- action.type, parmi : CREER_PROJET, MODIFIER_PROJET, DEMARRER_PROJET, SUSPENDRE_PROJET, REPRENDRE_PROJET, "
                    + "CLOTURER_PROJET, ARCHIVER_PROJET, CREER_SPRINT, MODIFIER_SPRINT, DEMARRER_SPRINT, CLOTURER_SPRINT, "
                    + "SUPPRIMER_SPRINT, CREER_JALON, MODIFIER_JALON, ATTEINDRE_JALON, ANNULER_JALON, SUPPRIMER_JALON, CREER_TACHE, "
                    + "MODIFIER_TACHE, AFFECTER_TACHE, DESAFFECTER_TACHE, CHANGER_STATUT_TACHE, ARCHIVER_TACHE, RESTAURER_TACHE, "
                    + "SUPPRIMER_TACHE.\n"
                    + "- Désigne le projet par son titre (champ projet), la tâche par son code comme T-12 (champ tache), le sprint "
                    + "par son nom (champ sprint, ou « backlog » pour retirer une tâche de son sprint), le jalon par son titre "
                    + "(champ jalon), les personnes par leur nom tel qu'il apparaît dans l'équipe (champ personnes). Reprends les "
                    + "désignations du chef de projet si elles ne figurent pas dans les données.\n"
                    + "- Pour une création ou une modification, mets les nouvelles valeurs dans titre (projet, tâche, jalon), nom "
                    + "(sprint), description, objectif, priorite (CRITIQUE, HAUTE, NORMALE, BASSE), statut (A_FAIRE, EN_COURS, "
                    + "EN_TEST, BLOQUEE, TERMINEE, REPORTEE), dateDebut, dateFin, dateEcheance, heuresEstimees, capaciteHeures, "
                    + "equipe. Dates au format AAAA-MM-JJ : calcule les dates relatives (« demain », « vendredi prochain », « dans "
                    + "2 semaines ») à partir de dateDuJour.\n"
                    + "- Ne remplis que les champs que le chef de projet a donnés ou qui découlent directement de sa demande : "
                    + "n'invente aucune valeur.";

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

    /** Schema de la reponse globale : celui de schemaReponse(), plus une action facultative a proposer. */
    public static Map<String, Object> schemaReponsePortefeuille() {
        Map<String, Object> proprietesAction = new LinkedHashMap<>();
        Map<String, Object> type = new LinkedHashMap<>();
        type.put("type", "string");
        type.put("enum", Arrays.asList("CREER_PROJET", "MODIFIER_PROJET", "DEMARRER_PROJET", "SUSPENDRE_PROJET",
                "REPRENDRE_PROJET", "CLOTURER_PROJET", "ARCHIVER_PROJET", "CREER_SPRINT", "MODIFIER_SPRINT", "DEMARRER_SPRINT",
                "CLOTURER_SPRINT", "SUPPRIMER_SPRINT", "CREER_JALON", "MODIFIER_JALON", "ATTEINDRE_JALON", "ANNULER_JALON",
                "SUPPRIMER_JALON", "CREER_TACHE", "MODIFIER_TACHE", "AFFECTER_TACHE", "DESAFFECTER_TACHE",
                "CHANGER_STATUT_TACHE", "ARCHIVER_TACHE", "RESTAURER_TACHE", "SUPPRIMER_TACHE"));
        proprietesAction.put("type", type);
        for (String champ : Arrays.asList("projet", "tache", "sprint", "jalon", "titre", "nom", "description", "objectif",
                "priorite", "statut", "dateDebut", "dateFin", "dateEcheance", "equipe")) {
            proprietesAction.put(champ, typeSimple("string"));
        }
        proprietesAction.put("heuresEstimees", typeSimple("number"));
        proprietesAction.put("capaciteHeures", typeSimple("number"));
        Map<String, Object> personnes = new LinkedHashMap<>();
        personnes.put("type", "array");
        personnes.put("items", typeSimple("string"));
        proprietesAction.put("personnes", personnes);

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "object");
        action.put("properties", proprietesAction);
        action.put("required", Arrays.asList("type"));

        Map<String, Object> racine = schemaReponse();
        @SuppressWarnings("unchecked")
        Map<String, Object> proprietes = (Map<String, Object>) racine.get("properties");
        proprietes.put("action", action);
        return racine;
    }

    private static Map<String, Object> typeSimple(String type) {
        Map<String, Object> noeud = new LinkedHashMap<>();
        noeud.put("type", type);
        return noeud;
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
