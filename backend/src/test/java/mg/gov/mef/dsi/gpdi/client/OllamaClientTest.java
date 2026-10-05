package mg.gov.mef.dsi.gpdi.client;

import mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaClientTest {

    private static final Map<String, Object> SCHEMA = Collections.<String, Object>singletonMap("type", "object");

    private MockRestServiceServer serveur;
    private OllamaClient client;

    @BeforeEach
    void initClient() {
        RestTemplate restTemplate = new RestTemplate();
        serveur = MockRestServiceServer.createServer(restTemplate);
        client = new OllamaClient(restTemplate, "http://localhost:11434/", "qwen3:8b", "30m", 8192);
    }

    @Test
    void genererJson_reponseValide_renvoieLeContenuEtEnvoieLesBonsParametres() {
        serveur.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("qwen3:8b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.think").value(false))
                .andExpect(jsonPath("$.keep_alive").value("30m"))
                .andExpect(jsonPath("$.options.num_ctx").value(8192))
                .andExpect(jsonPath("$.format.type").value("object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("consigne"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("question"))
                .andRespond(withSuccess("{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"ok\\\":true}\"}}",
                        MediaType.APPLICATION_JSON));

        String contenu = client.genererJson("consigne", "question", SCHEMA);

        assertThat(contenu).isEqualTo("{\"ok\":true}");
        serveur.verify();
    }

    @Test
    void genererJson_contenuVide_leveOllamaIndisponibleException() {
        serveur.expect(requestTo("http://localhost:11434/api/chat"))
                .andRespond(withSuccess("{\"message\":{\"role\":\"assistant\",\"content\":\"  \"}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.genererJson("consigne", "question", SCHEMA))
                .isInstanceOf(OllamaIndisponibleException.class);
    }

    @Test
    void genererJson_reponseSansMessage_leveOllamaIndisponibleException() {
        serveur.expect(requestTo("http://localhost:11434/api/chat"))
                .andRespond(withSuccess("{\"done\":true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.genererJson("consigne", "question", SCHEMA))
                .isInstanceOf(OllamaIndisponibleException.class);
    }

    @Test
    void genererJson_erreurServeur_leveOllamaIndisponibleException() {
        serveur.expect(requestTo("http://localhost:11434/api/chat"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.genererJson("consigne", "question", SCHEMA))
                .isInstanceOf(OllamaIndisponibleException.class)
                .hasCauseInstanceOf(org.springframework.web.client.RestClientException.class);
    }
}
