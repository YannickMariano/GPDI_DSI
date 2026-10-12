package mg.gov.mef.dsi.gpdi.client;

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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Client du serveur Ollama (modele de langage execute en local). Ne connait aucune regle metier :
 * il envoie une consigne, impose un schema JSON a la reponse et renvoie le JSON brut, que
 * l'appelant valide et interprete.
 * <p>
 * Il construit son propre {@link RestTemplate} (non expose comme bean) car les appels a un modele
 * de langage ont besoin d'un timeout de lecture bien plus long que celui du RestTemplate partage
 * (Keycloak) ; declarer un second bean casserait l'injection par type de ce dernier.
 */
@Component
@ConditionalOnProperty(name = "dsi.gpdi.ia.fournisseur", havingValue = "ollama", matchIfMissing = true)
public class OllamaClient implements ModeleIaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    /** Temperature basse : on veut des sorties structurees stables, pas de la creativite. */
    private static final double TEMPERATURE = 0.2;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String modele;
    private final String keepAlive;
    private final int numCtx;

    @Autowired
    public OllamaClient(@Value("${dsi.gpdi.ia.ollama.base-url}") String baseUrl,
                        @Value("${dsi.gpdi.ia.ollama.model}") String modele,
                        @Value("${dsi.gpdi.ia.ollama.timeout-seconds}") int timeoutSecondes,
                        @Value("${dsi.gpdi.ia.ollama.keep-alive}") String keepAlive,
                        @Value("${dsi.gpdi.ia.ollama.num-ctx}") int numCtx) {
        this(construireRestTemplate(timeoutSecondes), baseUrl, modele, keepAlive, numCtx);
    }

    OllamaClient(RestTemplate restTemplate, String baseUrl, String modele, String keepAlive, int numCtx) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.modele = modele;
        this.keepAlive = keepAlive;
        this.numCtx = numCtx;
    }

    /** Nom du modele utilise, a tracer avec chaque analyse pour pouvoir comparer les resultats dans le temps. */
    @Override
    public String getModele() {
        return modele;
    }

    /**
     * Envoie une consigne au modele et renvoie sa reponse, contrainte par {@code schemaJson}
     * (structured outputs d'Ollama). Le mode "reflexion" du modele est desactive : il multiplie
     * la duree de reponse sans apporter de gain pour de l'extraction structuree.
     *
     * @throws OllamaIndisponibleException si Ollama est injoignable, en erreur ou renvoie une reponse vide
     */
    @Override
    public String genererJson(String consigneSysteme, String consigneUtilisateur, Map<String, Object> schemaJson) {
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", TEMPERATURE);
        options.put("num_ctx", numCtx);

        Map<String, Object> corps = new HashMap<>();
        corps.put("model", modele);
        corps.put("stream", false);
        corps.put("think", false);
        corps.put("keep_alive", keepAlive);
        corps.put("format", schemaJson);
        corps.put("options", options);
        corps.put("messages", Arrays.asList(
                message("system", consigneSysteme),
                message("user", consigneUtilisateur)));

        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.APPLICATION_JSON);

        long debut = System.currentTimeMillis();
        try {
            ResponseEntity<Map<String, Object>> reponse = restTemplate.exchange(
                    baseUrl + "/api/chat", HttpMethod.POST, new HttpEntity<>(corps, entetes),
                    new ParameterizedTypeReference<Map<String, Object>>() { });
            String contenu = extraireContenu(reponse.getBody());
            log.info("Appel Ollama ({}) termine en {} ms", modele, System.currentTimeMillis() - debut);
            return contenu;
        } catch (RestClientException e) {
            throw new OllamaIndisponibleException(
                    "L'assistant IA est indisponible : le serveur Ollama n'a pas répondu ou a renvoyé une erreur.", e);
        }
    }

    private String extraireContenu(Map<String, Object> corpsReponse) {
        Object message = corpsReponse == null ? null : corpsReponse.get("message");
        Object contenu = message instanceof Map ? ((Map<?, ?>) message).get("content") : null;
        if (!(contenu instanceof String) || !StringUtils.hasText((String) contenu)) {
            throw new OllamaIndisponibleException("L'assistant IA a renvoyé une réponse vide.");
        }
        return (String) contenu;
    }

    private Map<String, Object> message(String role, String contenu) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        message.put("content", contenu);
        return message;
    }

    private static RestTemplate construireRestTemplate(int timeoutSecondes) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(timeoutSecondes * 1_000);
        factory.setOutputStreaming(false);
        return new RestTemplate(factory);
    }
}
