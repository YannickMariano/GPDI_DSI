package mg.gov.mef.dsi.gpdi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque;
import mg.gov.mef.dsi.gpdi.service.impl.ReponseSuggestionRisqueServiceImpl;
import mg.gov.mef.dsi.gpdi.util.SuggestionRisqueConsignes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de qualite du prompt avec le vrai modele : uniquement si OLLAMA_REEL=true (jamais en CI).
 * Le contexte est celui de DetectionRisqueOllamaReelTest (membres 10, 11, 12 ; taches 101 et 102).
 */
@EnabledIfEnvironmentVariable(named = "OLLAMA_REEL", matches = "true")
class SuggestionRisqueOllamaReelTest {

    private static final String RISQUE = "{\"id\":20,\"libelle\":\"Taches bloquees et en retard\","
            + "\"description\":\"Plusieurs taches sont bloquees ou en retard, ce qui menace la progression globale du projet.\","
            + "\"probabilite\":5,\"impact\":5,\"niveau\":\"CRITIQUE\","
            + "\"parade\":\"Analyser les causes des blocages et prioriser les taches critiques.\"}";

    @Test
    void suggestion_avecLeVraiModele_produitDesSolutionsExploitables() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode projet = mapper.readTree(DetectionRisqueOllamaReelTest.CONTEXTE);
        JsonNode risque = mapper.readTree(RISQUE);

        OllamaClient client = new OllamaClient("http://localhost:11434", "qwen3:8b", 600, "30m", 8192);
        long debut = System.currentTimeMillis();
        String brut = client.genererJson(SuggestionRisqueConsignes.consigneSysteme(),
                SuggestionRisqueConsignes.consigneUtilisateur(projet, risque), SuggestionRisqueConsignes.schemaReponse());
        long duree = (System.currentTimeMillis() - debut) / 1000;

        ReponseSuggestionRisque reponse = new ReponseSuggestionRisqueServiceImpl(mapper).interpreter(brut,
                new HashSet<>(Arrays.asList(10L, 11L, 12L)), new HashSet<>(Arrays.asList(101L, 102L)));

        System.out.println("=== DUREE " + duree + " s");
        System.out.println("=== REPONSE BRUTE\n" + brut);
        reponse.getSolutions().forEach(solution -> System.out.println("=== RETENUE : " + solution.getType() + " | "
                + solution.getLibelle() + " | tache=" + solution.getIdTache() + " assigne=" + solution.getIdAssigne()
                + " | gain=" + solution.getGain()));
        assertThat(reponse.getSolutions()).isNotEmpty();
    }
}
