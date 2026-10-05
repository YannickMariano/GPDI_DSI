package mg.gov.mef.dsi.gpdi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SuggestionRisqueConsignesTest {

    @Test
    void consigneSysteme_annonceLePlafondEtLesDeuxTypesDAction() {
        assertThat(SuggestionRisqueConsignes.consigneSysteme())
                .contains("de 2 à " + SuggestionRisqueConsignes.MAX_SOLUTIONS + " solutions")
                .contains("REASSIGNER_TACHE")
                .contains("CREER_TACHE")
                .contains("N'invente aucun");
    }

    @Test
    void consigneUtilisateur_integreLeProjetEtLeRisque() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode projet = mapper.readTree("{\"projet\":{\"id\":1}}");
        JsonNode risque = mapper.readTree("{\"libelle\":\"Retard\"}");

        assertThat(SuggestionRisqueConsignes.consigneUtilisateur(projet, risque))
                .contains("{\"projet\":{\"id\":1}}")
                .contains("{\"libelle\":\"Retard\"}");
    }

    @Test
    @SuppressWarnings("unchecked")
    void schemaReponse_imposeLesChampsDUneSolutionEtLesTypesDAction() {
        Map<String, Object> schema = SuggestionRisqueConsignes.schemaReponse();

        assertThat((List<String>) schema.get("required")).containsExactly("solutions");
        Map<String, Object> proprietes = (Map<String, Object>) schema.get("properties");
        Map<String, Object> solution = (Map<String, Object>) ((Map<String, Object>) proprietes.get("solutions")).get("items");
        Map<String, Object> proprietesSolution = (Map<String, Object>) solution.get("properties");

        assertThat(proprietesSolution.keySet()).containsExactly("libelle", "justification", "gain", "typeAction",
                "titreTache", "descriptionTache", "tempsEstime", "idTache", "idAssigne");
        assertThat((Map<String, Object>) proprietesSolution.get("typeAction"))
                .containsEntry("enum", Arrays.asList("CREER_TACHE", "REASSIGNER_TACHE"));
        assertThat((List<String>) solution.get("required")).containsExactly("libelle", "justification", "gain", "typeAction");
    }
}
