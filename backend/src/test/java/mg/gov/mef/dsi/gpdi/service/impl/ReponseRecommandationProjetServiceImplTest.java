package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet.RecommandationProposee;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandationProjet;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReponseRecommandationProjetServiceImplTest {

    private ReponseRecommandationProjetServiceImpl service;
    private Set<Long> taches;
    private Set<Long> membres;
    private List<String> aucuneDejaProposee;

    @BeforeEach
    void setUp() {
        service = new ReponseRecommandationProjetServiceImpl(new ObjectMapper());
        taches = new HashSet<>(Arrays.asList(100L, 101L));
        membres = new HashSet<>(Arrays.asList(10L, 11L));
        aucuneDejaProposee = Collections.emptyList();
    }

    private static String recommandations(String... recommandations) {
        return "{\"recommandations\":[" + String.join(",", recommandations) + "]}";
    }

    private static String reaffectation(String libelle, String idTache, String idAssigne) {
        return "{\"type\":\"REASSIGNER_TACHE\",\"libelle\":\"" + libelle + "\",\"justification\":\"Parce que\","
                + "\"preuve\":\"Preuve\",\"idTache\":" + idTache + ",\"idAssigne\":" + idAssigne + "}";
    }

    private static String retraitDuSprint(String libelle, String idTache) {
        return "{\"type\":\"RETIRER_DU_SPRINT\",\"libelle\":\"" + libelle + "\",\"justification\":\"Parce que\","
                + "\"preuve\":\"Preuve\",\"idTache\":" + idTache + "}";
    }

    private static String ajustementEstimation(String libelle, String idTache, String nouvelleEstimation) {
        return "{\"type\":\"AJUSTER_ESTIMATION\",\"libelle\":\"" + libelle + "\",\"justification\":\"Parce que\","
                + "\"idTache\":" + idTache + ",\"nouvelleEstimation\":" + nouvelleEstimation + "}";
    }

    @Test
    void interpreter_reaffectationValide_estConservee() {
        ReponseRecommandationProjet reponse = service.interpreter(
                recommandations(reaffectation("Confier T-101 a Lala", "100", "11")), taches, membres, aucuneDejaProposee);

        RecommandationProposee recommandation = reponse.getRecommandations().get(0);
        assertThat(recommandation.getType()).isEqualTo(TypeActionRecommandationProjet.REASSIGNER_TACHE);
        assertThat(recommandation.getIdTache()).isEqualTo(100L);
        assertThat(recommandation.getIdAssigne()).isEqualTo(11L);
        assertThat(recommandation.getPreuve()).isEqualTo("Preuve");
    }

    @Test
    void interpreter_reaffectationVersMembreInconnu_estEcartee() {
        ReponseRecommandationProjet reponse = service.interpreter(
                recommandations(reaffectation("Confier T-101 a Inconnu", "100", "999")), taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).isEmpty();
    }

    @Test
    void interpreter_tacheInconnue_estEcartee() {
        ReponseRecommandationProjet reponse = service.interpreter(
                recommandations(retraitDuSprint("Retirer T-999", "999")), taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).isEmpty();
    }

    @Test
    void interpreter_retraitDuSprintValide_estConservee() {
        ReponseRecommandationProjet reponse = service.interpreter(
                recommandations(retraitDuSprint("Retirer T-100 du sprint", "100")), taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).hasSize(1);
        assertThat(reponse.getRecommandations().get(0).getType()).isEqualTo(TypeActionRecommandationProjet.RETIRER_DU_SPRINT);
        assertThat(reponse.getRecommandations().get(0).getIdAssigne()).isNull();
    }

    @Test
    void interpreter_ajustementEstimationValide_estConservee() {
        ReponseRecommandationProjet reponse = service.interpreter(
                recommandations(ajustementEstimation("Reestimer T-100", "100", "12.5")), taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).hasSize(1);
        assertThat(reponse.getRecommandations().get(0).getNouvelleEstimation()).isEqualTo(12.5);
    }

    @Test
    void interpreter_estimationNegativeOuAberrante_estEcartee() {
        ReponseRecommandationProjet negative = service.interpreter(
                recommandations(ajustementEstimation("Reestimer T-100", "100", "-5")), taches, membres, aucuneDejaProposee);
        ReponseRecommandationProjet aberrante = service.interpreter(
                recommandations(ajustementEstimation("Reestimer T-100", "100", "5000")), taches, membres, aucuneDejaProposee);

        assertThat(negative.getRecommandations()).isEmpty();
        assertThat(aberrante.getRecommandations()).isEmpty();
    }

    @Test
    void interpreter_typeActionInconnu_estEcartee() {
        String recommandation = "{\"type\":\"SUPPRIMER_PROJET\",\"libelle\":\"Douteux\",\"justification\":\"Parce que\",\"idTache\":100}";

        ReponseRecommandationProjet reponse = service.interpreter(recommandations(recommandation), taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).isEmpty();
    }

    @Test
    void interpreter_libelleDejaPropose_estEcarteeCommeDoublon() {
        ReponseRecommandationProjet reponse = service.interpreter(
                recommandations(retraitDuSprint("Retirer T-100 du sprint", "100")), taches, membres,
                Collections.singletonList("Retirer T-100 du sprint"));

        assertThat(reponse.getRecommandations()).isEmpty();
    }

    @Test
    void interpreter_doublonAuSeinDeLaMemeReponse_neGardeQueLaPremiere() {
        ReponseRecommandationProjet reponse = service.interpreter(
                recommandations(retraitDuSprint("Retirer T-100 du sprint", "100"), retraitDuSprint("Retirer T-100 du sprint", "101")),
                taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).hasSize(1);
    }

    @Test
    void interpreter_aucuneRecommandationJustifiee_renvoieUneListeVideSansErreur() {
        ReponseRecommandationProjet reponse = service.interpreter(recommandations(), taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).isEmpty();
    }

    @Test
    void interpreter_plusDuPlafond_neGardeQueLesPremieres() {
        // Libelles nettement distincts (pas seulement un chiffre final) pour ne pas se faire ecarter comme doublons.
        String[] motifs = {"desengorger le backlog", "soulager la charge sprint", "corriger un ecart de planification",
                "limiter le risque de retard", "ameliorer la visibilite du jalon", "reequilibrer la charge d equipe",
                "fluidifier la fin de sprint"};
        String[] septRecommandations = new String[motifs.length];
        for (int i = 0; i < motifs.length; i++) {
            septRecommandations[i] = retraitDuSprint("Retirer T-100 pour " + motifs[i], "100");
        }

        ReponseRecommandationProjet reponse = service.interpreter(recommandations(septRecommandations), taches, membres, aucuneDejaProposee);

        assertThat(reponse.getRecommandations()).hasSize(6);
    }

    @Test
    void interpreter_reponseVide_leveReponseIaInvalideException() {
        assertThatThrownBy(() -> service.interpreter("", taches, membres, aucuneDejaProposee))
                .isInstanceOf(ReponseIaInvalideException.class);
    }

    @Test
    void interpreter_jsonSansListeDeRecommandations_leveReponseIaInvalideException() {
        assertThatThrownBy(() -> service.interpreter("{}", taches, membres, aucuneDejaProposee))
                .isInstanceOf(ReponseIaInvalideException.class);
    }
}
