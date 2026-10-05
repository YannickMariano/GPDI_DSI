package mg.gov.mef.dsi.gpdi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque;
import mg.gov.mef.dsi.gpdi.service.impl.ReponseDetectionRisqueServiceImpl;
import mg.gov.mef.dsi.gpdi.util.DetectionRisqueConsignes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de qualite du prompt avec le vrai modele : uniquement si OLLAMA_REEL=true (jamais en CI).
 * Premier appel a froid : jusqu'a plusieurs minutes.
 */
@EnabledIfEnvironmentVariable(named = "OLLAMA_REEL", matches = "true")
class DetectionRisqueOllamaReelTest {

    static final String CONTEXTE = "{\"dateDuJour\":\"2026-09-21\","
            + "\"projet\":{\"id\":1,\"titre\":\"Refonte du SI budgetaire\",\"statut\":\"EN_COURS\",\"priorite\":\"HAUTE\","
            + "\"dateDebut\":\"2026-05-01\",\"dateFin\":\"2026-10-15\",\"joursAvantEcheance\":24,\"progressionPourcent\":35},"
            + "\"equipe\":[{\"id\":10,\"nom\":\"Jean Rakoto\",\"role\":\"chef_projet\",\"fonction\":\"RESPONSABLE_PROJET\",\"tachesOuvertes\":2,\"heuresRestantes\":30.0},"
            + "{\"id\":11,\"nom\":\"Paul Randria\",\"role\":\"membre_equipe\",\"fonction\":\"MEMBRE\",\"tachesOuvertes\":9,\"heuresRestantes\":120.0},"
            + "{\"id\":12,\"nom\":\"Lala Rabe\",\"role\":\"membre_equipe\",\"fonction\":\"MEMBRE\",\"tachesOuvertes\":1,\"heuresRestantes\":6.0}],"
            + "\"taches\":{\"total\":30,\"parStatut\":{\"A_FAIRE\":8,\"EN_COURS\":6,\"BLOQUEE\":2,\"TERMINEE\":14},\"enRetard\":5,\"sansAssigne\":3,\"sansEstimation\":4,\"dansLeBacklog\":6,"
            + "\"aSurveiller\":[{\"id\":101,\"code\":\"T-101\",\"titre\":\"Migration des donnees\",\"statut\":\"BLOQUEE\",\"priorite\":\"CRITIQUE\",\"echeance\":\"2026-09-10\",\"joursDeRetard\":11,\"assignes\":[11],\"heuresEstimees\":40.0},"
            + "{\"id\":102,\"code\":\"T-102\",\"titre\":\"Recette utilisateur\",\"statut\":\"EN_COURS\",\"priorite\":\"HAUTE\",\"echeance\":\"2026-09-18\",\"joursDeRetard\":3,\"assignes\":[11],\"heuresEstimees\":24.0}]},"
            + "\"sprints\":[{\"id\":5,\"nom\":\"Sprint 4\",\"statut\":\"EN_COURS\",\"dateDebut\":\"2026-09-14\",\"dateFin\":\"2026-09-28\",\"capaciteHeures\":80,\"heuresRestantes\":150.0,"
            + "\"burndown\":[{\"date\":\"2026-09-15\",\"heuresRestantes\":150.0},{\"date\":\"2026-09-18\",\"heuresRestantes\":152.0},{\"date\":\"2026-09-21\",\"heuresRestantes\":150.0}]}],"
            + "\"jalons\":[{\"id\":3,\"titre\":\"Mise en recette\",\"statut\":\"EN_RETARD\",\"echeance\":\"2026-09-15\",\"joursAvantEcheance\":-6,\"livrables\":{\"ATTENDU\":2,\"LIVRE\":1}}],"
            + "\"risquesConnus\":[{\"id\":7,\"libelle\":\"Retard de la migration des donnees\",\"statut\":\"SOUS_CONTROLE\",\"niveau\":\"MOYEN\",\"probabilite\":3,\"impact\":3}]}";

    @Test
    void detection_avecLeVraiModele_produitUneReponseExploitable() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode contenu = mapper.readTree(CONTEXTE);
        ContexteProjetIa contexte = new ContexteProjetIa(contenu,
                new HashSet<>(Arrays.asList(10L, 11L, 12L)),
                new HashSet<>(Collections.singletonList(7L)),
                Collections.singletonList("Retard de la migration des donnees"));

        OllamaClient client = new OllamaClient("http://localhost:11434", "qwen3:8b", 600, "30m", 8192);
        long debut = System.currentTimeMillis();
        String brut = client.genererJson(DetectionRisqueConsignes.consigneSysteme(),
                DetectionRisqueConsignes.consigneUtilisateur(contenu), DetectionRisqueConsignes.schemaReponse());
        long duree = (System.currentTimeMillis() - debut) / 1000;

        ReponseDetectionRisque reponse = new ReponseDetectionRisqueServiceImpl(mapper).interpreter(brut, contexte);

        System.out.println("=== DUREE " + duree + " s");
        System.out.println("=== REPONSE BRUTE\n" + brut);
        System.out.println("=== RISQUES RETENUS : " + reponse.getNouveauxRisques().size()
                + " | REEVALUATIONS : " + reponse.getReevaluations().size()
                + " | CONFIANCE : " + reponse.getIndiceConfiance());
        assertThat(reponse.getNouveauxRisques()).isNotEmpty();
    }
}
