package mg.gov.mef.dsi.gpdi.client;

import java.util.Map;

/**
 * Modele de langage utilise par le module IA. Une seule implementation est active, choisie par
 * {@code dsi.gpdi.ia.fournisseur} : {@code ollama} (local, Qwen3:8b par defaut — valeur par defaut)
 * ou {@code gemini} (Gemini Flash en ligne via Google AI Studio).
 */
public interface ModeleIaClient {

    /** Nom du modele utilise, a tracer avec chaque analyse pour pouvoir comparer les resultats dans le temps. */
    String getModele();

    /**
     * Envoie une consigne au modele et renvoie sa reponse JSON brute, attendue conforme a {@code schemaJson}
     * (l'appelant la valide et l'interprete).
     *
     * @throws mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException si le modele est injoignable, en erreur ou renvoie une reponse vide
     */
    String genererJson(String consigneSysteme, String consigneUtilisateur, Map<String, Object> schemaJson);
}
