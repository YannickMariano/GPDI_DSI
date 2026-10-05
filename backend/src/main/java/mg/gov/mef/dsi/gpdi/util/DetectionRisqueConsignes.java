package mg.gov.mef.dsi.gpdi.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consignes envoyees au modele pour la detection des risques d'un projet, et schema JSON impose a sa
 * reponse. L'ordre des proprietes du schema est celui dans lequel le modele les produit : les textes
 * (libelle, description) sont generes avant les notes (probabilite, impact) pour qu'il les justifie
 * avant de les chiffrer.
 */
public final class DetectionRisqueConsignes {

    /** Plafond de nouveaux risques par analyse, annonce au modele et reapplique a sa reponse. */
    public static final int MAX_NOUVEAUX_RISQUES = 5;

    private static final String CONSIGNE_SYSTEME =
            "Tu es un assistant expert en pilotage de projets informatiques pour une administration. "
                    + "A partir des donnees JSON d'un projet, tu identifies les risques qui menacent reellement sa reussite "
                    + "(delais, charge de l'equipe, taches bloquees ou en retard, dependance a une personne, jalons et livrables).\n"
                    + "\n"
                    + "Regles :\n"
                    + "- Base-toi uniquement sur les donnees fournies. N'invente aucun fait, chiffre, nom ou identifiant.\n"
                    + "- Reponds uniquement par un objet JSON conforme au schema demande, redige en francais.\n"
                    + "- N'ajoute jamais un risque deja present dans \"risquesConnus\", meme reformule, quel que soit son statut.\n"
                    + "- Propose au plus " + MAX_NOUVEAUX_RISQUES + " nouveaux risques, uniquement s'ils sont etayes par les donnees. "
                    + "Si rien ne le justifie, renvoie une liste vide.\n"
                    + "- probabilite et impact sont des entiers de 1 a 5 (1 = tres faible, 5 = tres eleve). "
                    + "La probabilite est la chance que le risque se realise ; l'impact est sa gravite sur les delais, le cout ou la qualite s'il se realise.\n"
                    + "- parade : une mesure preventive courte, en une phrase.\n"
                    + "- idPorteur : le champ \"id\" d'un membre de la liste \"equipe\" le mieux place pour surveiller ce risque. "
                    + "Omets ce champ si tu hesites.\n"
                    + "- reevaluations : uniquement pour les risques de \"risquesConnus\" dont le statut est SOUS_CONTROLE. "
                    + "Redonne leur probabilite et leur impact a jour selon l'etat actuel du projet. "
                    + "Ne cite aucun autre identifiant.\n"
                    + "- synthese : deux ou trois phrases sur l'etat de sante general du projet.\n"
                    + "- indiceConfiance : un nombre entre 0 et 1.";

    private DetectionRisqueConsignes() {
    }

    public static String consigneSysteme() {
        return CONSIGNE_SYSTEME;
    }

    public static String consigneUtilisateur(JsonNode contexteProjet) {
        return "Donnees du projet (JSON) :\n" + contexteProjet.toString()
                + "\n\nDetecte les risques de ce projet et reevalue ceux qui sont SOUS_CONTROLE.";
    }

    public static Map<String, Object> schemaReponse() {
        Map<String, Object> risque = new LinkedHashMap<>();
        risque.put("libelle", chaine());
        risque.put("description", chaine());
        risque.put("probabilite", entierBorne(1, 5));
        risque.put("impact", entierBorne(1, 5));
        risque.put("parade", chaine());
        risque.put("idPorteur", type("integer"));

        Map<String, Object> reevaluation = new LinkedHashMap<>();
        reevaluation.put("idRisque", type("integer"));
        reevaluation.put("probabilite", entierBorne(1, 5));
        reevaluation.put("impact", entierBorne(1, 5));

        Map<String, Object> racine = new LinkedHashMap<>();
        racine.put("nouveauxRisques", tableau(objet(risque, "libelle", "description", "probabilite", "impact", "parade")));
        racine.put("reevaluations", tableau(objet(reevaluation, "idRisque", "probabilite", "impact")));
        racine.put("synthese", chaine());
        racine.put("indiceConfiance", type("number"));

        return objet(racine, "nouveauxRisques", "reevaluations", "synthese", "indiceConfiance");
    }

    private static Map<String, Object> objet(Map<String, Object> proprietes, String... requis) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", proprietes);
        schema.put("required", Arrays.asList(requis));
        return schema;
    }

    private static Map<String, Object> tableau(Map<String, Object> elements) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", elements);
        return schema;
    }

    private static Map<String, Object> entierBorne(int minimum, int maximum) {
        Map<String, Object> schema = type("integer");
        schema.put("minimum", minimum);
        schema.put("maximum", maximum);
        return schema;
    }

    private static Map<String, Object> chaine() {
        return type("string");
    }

    private static Map<String, Object> type(String type) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        return schema;
    }
}
