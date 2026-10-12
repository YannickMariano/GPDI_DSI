package mg.gov.mef.dsi.gpdi.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client de l'API Gemini (Google AI Studio), actif quand {@code dsi.gpdi.ia.fournisseur=gemini}.
 * Meme contrat que {@link OllamaClient} : il envoie une consigne et renvoie le JSON brut, que
 * l'appelant valide et interprete.
 * <p>
 * La reponse est forcee en JSON ({@code responseMimeType: application/json}) et le schema attendu
 * est ajoute a la consigne systeme (le sous-ensemble de JSON Schema accepte nativement par Gemini
 * ne couvre pas forcement tous les schemas des consignes GPDI).
 * <p>
 * Attention : avec ce fournisseur, le contexte du projet (titres, taches, risques...) quitte la
 * machine et est envoye a Google.
 */
@Component
@ConditionalOnProperty(name = "dsi.gpdi.ia.fournisseur", havingValue = "gemini")
public class GeminiClient implements ModeleIaClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    /** Temperature basse : on veut des sorties structurees stables, pas de la creativite. */
    private static final double TEMPERATURE = 0.2;

    /** Nombre total d'appels tentes quand Gemini repond 429 (quota par minute) ou 503 (modele surcharge). */
    private static final int NOMBRE_TENTATIVES = 3;
    private static final long ATTENTE_ENTRE_TENTATIVES_MS = 3_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String cleApi;
    private final String modele;

    @Autowired
    public GeminiClient(ObjectMapper objectMapper,
                        @Value("${dsi.gpdi.ia.gemini.base-url}") String baseUrl,
                        @Value("${dsi.gpdi.ia.gemini.api-key}") String cleApi,
                        @Value("${dsi.gpdi.ia.gemini.model}") String modele,
                        @Value("${dsi.gpdi.ia.gemini.timeout-seconds}") int timeoutSecondes) {
        this(construireRestTemplate(timeoutSecondes), objectMapper, baseUrl, cleApi, modele);
    }

    GeminiClient(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl, String cleApi, String modele) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.cleApi = cleApi;
        this.modele = modele;
    }

    @Override
    public String getModele() {
        return modele;
    }

    @Override
    public String genererJson(String consigneSysteme, String consigneUtilisateur, Map<String, Object> schemaJson) {
        if (!StringUtils.hasText(cleApi)) {
            throw new OllamaIndisponibleException(
                    "L'assistant IA est indisponible : la clé API Gemini (GEMINI_API_KEY) n'est pas configurée.");
        }

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("temperature", TEMPERATURE);
        configuration.put("responseMimeType", "application/json");

        Map<String, Object> contenuUtilisateur = new HashMap<>();
        contenuUtilisateur.put("role", "user");
        contenuUtilisateur.put("parts", Collections.singletonList(texte(consigneUtilisateur)));

        Map<String, Object> corps = new HashMap<>();
        corps.put("systemInstruction", Collections.singletonMap("parts",
                Collections.singletonList(texte(consigneAvecSchema(consigneSysteme, schemaJson)))));
        corps.put("contents", Collections.singletonList(contenuUtilisateur));
        corps.put("generationConfig", configuration);

        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.APPLICATION_JSON);
        entetes.set("x-goog-api-key", cleApi);

        HttpEntity<Map<String, Object>> requete = new HttpEntity<>(corps, entetes);
        for (int tentative = 1; ; tentative++) {
            long debut = System.currentTimeMillis();
            try {
                ResponseEntity<Map<String, Object>> reponse = restTemplate.exchange(
                        baseUrl + "/models/" + modele + ":generateContent", HttpMethod.POST, requete,
                        new ParameterizedTypeReference<Map<String, Object>>() { });
                String contenu = extraireContenu(reponse.getBody());
                log.info("Appel Gemini ({}) termine en {} ms", modele, System.currentTimeMillis() - debut);
                return contenu;
            } catch (HttpStatusCodeException e) {
                int statut = e.getRawStatusCode();
                log.warn("Appel Gemini ({}) en erreur HTTP {} (tentative {}/{}) apres {} ms : {}", modele, statut, tentative,
                        NOMBRE_TENTATIVES, System.currentTimeMillis() - debut, extrait(e.getResponseBodyAsString()));
                // Surcharge du modele (503) ou quota par minute du niveau gratuit (429) : souvent passager, on reessaie.
                if ((statut == 429 || statut == 503) && tentative < NOMBRE_TENTATIVES) {
                    patienter(tentative);
                    continue;
                }
                throw new OllamaIndisponibleException(messageErreurHttp(statut), e);
            } catch (ResourceAccessException e) {
                log.warn("Appel Gemini ({}) sans reponse apres {} ms : {}", modele, System.currentTimeMillis() - debut, e.getMessage());
                throw new OllamaIndisponibleException(
                        "L'assistant IA est indisponible : l'API Gemini n'a pas répondu à temps (délai dépassé ou réseau indisponible).", e);
            } catch (RestClientException e) {
                log.warn("Appel Gemini ({}) en erreur : {}", modele, e.getMessage());
                throw new OllamaIndisponibleException(
                        "L'assistant IA est indisponible : l'API Gemini a renvoyé une erreur.", e);
            }
        }
    }

    private String messageErreurHttp(int statut) {
        switch (statut) {
            case 400:
                return "L'assistant IA est indisponible : l'API Gemini a refusé la demande (requête invalide ou trop volumineuse).";
            case 401:
            case 403:
                return "L'assistant IA est indisponible : la clé API Gemini est invalide ou n'a pas accès à ce modèle.";
            case 404:
                return "L'assistant IA est indisponible : le modèle Gemini « " + modele + " » est introuvable (vérifier GEMINI_MODEL).";
            case 429:
                return "L'assistant IA est indisponible : quota gratuit Gemini atteint (trop de requêtes). Réessayez dans une minute.";
            case 500:
            case 503:
                return "L'assistant IA est indisponible : le modèle Gemini est surchargé pour le moment. Réessayez dans quelques secondes.";
            default:
                return "L'assistant IA est indisponible : l'API Gemini a renvoyé une erreur (HTTP " + statut + ").";
        }
    }

    /** Attente croissante entre deux tentatives (3 s, puis 6 s...). */
    private void patienter(int tentative) {
        try {
            Thread.sleep(ATTENTE_ENTRE_TENTATIVES_MS * tentative);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String extrait(String corps) {
        if (corps == null) {
            return "";
        }
        String uneLigne = corps.replaceAll("\\s+", " ");
        return uneLigne.length() > 500 ? uneLigne.substring(0, 500) + "…" : uneLigne;
    }

    private String consigneAvecSchema(String consigneSysteme, Map<String, Object> schemaJson) {
        try {
            return consigneSysteme
                    + "\n\nRéponds uniquement par un objet JSON valide, sans texte autour ni bloc de code, "
                    + "strictement conforme à ce schéma JSON :\n"
                    + objectMapper.writeValueAsString(schemaJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Schéma JSON de réponse IA non sérialisable", e);
        }
    }

    /** Concatene le texte des "parts" du premier candidat (en ignorant d'eventuelles parts de reflexion). */
    private String extraireContenu(Map<String, Object> corpsReponse) {
        Object candidats = corpsReponse == null ? null : corpsReponse.get("candidates");
        Object premier = candidats instanceof List && !((List<?>) candidats).isEmpty() ? ((List<?>) candidats).get(0) : null;
        Object contenu = premier instanceof Map ? ((Map<?, ?>) premier).get("content") : null;
        Object parts = contenu instanceof Map ? ((Map<?, ?>) contenu).get("parts") : null;

        StringBuilder texte = new StringBuilder();
        if (parts instanceof List) {
            for (Object part : (List<?>) parts) {
                if (part instanceof Map && !Boolean.TRUE.equals(((Map<?, ?>) part).get("thought"))
                        && ((Map<?, ?>) part).get("text") instanceof String) {
                    texte.append((String) ((Map<?, ?>) part).get("text"));
                }
            }
        }
        if (!StringUtils.hasText(texte.toString())) {
            throw new OllamaIndisponibleException("L'assistant IA a renvoyé une réponse vide.");
        }
        return retirerBlocDeCode(texte.toString().trim());
    }

    /** Certains modeles entourent malgre tout le JSON d'un bloc ```json ... ``` : on le retire. */
    private String retirerBlocDeCode(String contenu) {
        if (!contenu.startsWith("```")) {
            return contenu;
        }
        int debut = contenu.indexOf('\n');
        int fin = contenu.lastIndexOf("```");
        return debut >= 0 && fin > debut ? contenu.substring(debut + 1, fin).trim() : contenu;
    }

    private Map<String, Object> texte(String contenu) {
        return Collections.singletonMap("text", contenu);
    }

    private static RestTemplate construireRestTemplate(int timeoutSecondes) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(timeoutSecondes * 1_000);
        factory.setOutputStreaming(false);
        return new RestTemplate(factory);
    }
}
