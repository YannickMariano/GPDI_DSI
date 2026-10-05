package mg.gov.mef.dsi.gpdi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DetectionRisqueConsignesTest {

    @Test
    void consigneSysteme_annonceLePlafondDeRisquesEtLesRegles() {
        String consigne = DetectionRisqueConsignes.consigneSysteme();

        assertThat(consigne)
                .contains("au plus " + DetectionRisqueConsignes.MAX_NOUVEAUX_RISQUES + " nouveaux risques")
                .contains("SOUS_CONTROLE")
                .contains("N'invente aucun");
    }

    @Test
    void consigneUtilisateur_integreLeContexteEnJson() throws IOException {
        JsonNode contexte = new ObjectMapper().readTree("{\"projet\":{\"id\":1}}");

        assertThat(DetectionRisqueConsignes.consigneUtilisateur(contexte)).contains("{\"projet\":{\"id\":1}}");
    }

    @Test
    @SuppressWarnings("unchecked")
    void schemaReponse_imposeLesChampsAttendusEtLesNotesDe1a5() {
        Map<String, Object> schema = DetectionRisqueConsignes.schemaReponse();

        assertThat(schema.get("type")).isEqualTo("object");
        assertThat((List<String>) schema.get("required"))
                .containsExactly("nouveauxRisques", "reevaluations", "synthese", "indiceConfiance");

        Map<String, Object> proprietes = (Map<String, Object>) schema.get("properties");
        Map<String, Object> risque = (Map<String, Object>) ((Map<String, Object>) proprietes.get("nouveauxRisques")).get("items");
        Map<String, Object> proprietesRisque = (Map<String, Object>) risque.get("properties");

        assertThat(proprietesRisque.keySet())
                .containsExactly("libelle", "description", "probabilite", "impact", "parade", "idPorteur");
        assertThat((Map<String, Object>) proprietesRisque.get("probabilite"))
                .containsEntry("minimum", 1)
                .containsEntry("maximum", 5);
        assertThat((List<String>) risque.get("required"))
                .doesNotContain("idPorteur")
                .containsAll(Arrays.asList("libelle", "probabilite", "impact"));
    }
}
