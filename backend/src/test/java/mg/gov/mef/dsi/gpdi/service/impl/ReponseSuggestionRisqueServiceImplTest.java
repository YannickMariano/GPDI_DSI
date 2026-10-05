package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque.SolutionProposee;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandation;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReponseSuggestionRisqueServiceImplTest {

    private ReponseSuggestionRisqueServiceImpl service;
    private Set<Long> membres;
    private Set<Long> taches;

    @BeforeEach
    void setUp() {
        service = new ReponseSuggestionRisqueServiceImpl(new ObjectMapper());
        membres = new HashSet<>(Arrays.asList(10L, 11L));
        taches = new HashSet<>(Arrays.asList(100L, 101L));
    }

    private static String solutions(String... solutions) {
        return "{\"solutions\":[" + String.join(",", solutions) + "]}";
    }

    private static String reaffectation(String libelle, String idTache, String idAssigne) {
        return "{\"libelle\":\"" + libelle + "\",\"justification\":\"Parce que\",\"gain\":\"2 jours\","
                + "\"typeAction\":\"REASSIGNER_TACHE\",\"idTache\":" + idTache + ",\"idAssigne\":" + idAssigne + "}";
    }

    private static String creation(String libelle, String complement) {
        return "{\"libelle\":\"" + libelle + "\",\"justification\":\"Parce que\",\"gain\":\"1 jour\","
                + "\"typeAction\":\"CREER_TACHE\"" + complement + "}";
    }

    @Test
    void interpreter_reaffectationValide_estConservee() {
        ReponseSuggestionRisque reponse = service.interpreter(
                solutions(reaffectation("Confier T-101 a Lala", "100", "11")), membres, taches);

        SolutionProposee solution = reponse.getSolutions().get(0);
        assertThat(solution.getType()).isEqualTo(TypeActionRecommandation.REASSIGNER_TACHE);
        assertThat(solution.getIdTache()).isEqualTo(100L);
        assertThat(solution.getIdAssigne()).isEqualTo(11L);
        assertThat(solution.getLibelle()).isEqualTo("Confier T-101 a Lala");
        assertThat(solution.getJustification()).isEqualTo("Parce que");
        assertThat(solution.getGain()).isEqualTo("2 jours");
        assertThat(solution.getTitreTache()).isNull();
    }

    @Test
    void interpreter_reaffectationVersTacheOuMembreInconnu_devientUneTacheDeSuivi() {
        ReponseSuggestionRisque reponse = service.interpreter(solutions(
                reaffectation("Tache inconnue", "999", "11"),
                reaffectation("Membre inconnu", "100", "999")), membres, taches);

        assertThat(reponse.getSolutions()).hasSize(2);
        SolutionProposee tacheInconnue = reponse.getSolutions().get(0);
        assertThat(tacheInconnue.getType()).isEqualTo(TypeActionRecommandation.CREER_TACHE);
        assertThat(tacheInconnue.getIdTache()).isNull();
        assertThat(tacheInconnue.getTitreTache()).isEqualTo("Tache inconnue");
        assertThat(tacheInconnue.getDescriptionTache()).isEqualTo("Parce que");
        assertThat(tacheInconnue.getIdAssigne()).isEqualTo(11L);
        SolutionProposee membreInconnu = reponse.getSolutions().get(1);
        assertThat(membreInconnu.getType()).isEqualTo(TypeActionRecommandation.CREER_TACHE);
        assertThat(membreInconnu.getIdAssigne()).isNull();
    }

    @Test
    void interpreter_creationDeTache_conserveTitreDescriptionEstimationEtAssigneValide() {
        ReponseSuggestionRisque reponse = service.interpreter(solutions(creation("Planifier une revue",
                ",\"titreTache\":\"Organiser la revue\",\"descriptionTache\":\"Revue hebdo\",\"tempsEstime\":3.5,\"idAssigne\":10")),
                membres, taches);

        SolutionProposee solution = reponse.getSolutions().get(0);
        assertThat(solution.getType()).isEqualTo(TypeActionRecommandation.CREER_TACHE);
        assertThat(solution.getTitreTache()).isEqualTo("Organiser la revue");
        assertThat(solution.getDescriptionTache()).isEqualTo("Revue hebdo");
        assertThat(solution.getTempsEstime()).isEqualTo(3.5);
        assertThat(solution.getIdAssigne()).isEqualTo(10L);
    }

    @Test
    void interpreter_estimationAberranteOuMembreInconnu_lesIgnore() {
        ReponseSuggestionRisque reponse = service.interpreter(solutions(
                creation("Premiere solution", ",\"tempsEstime\":-4,\"idAssigne\":999"),
                creation("Deuxieme approche differente", ",\"tempsEstime\":100000")), membres, taches);

        assertThat(reponse.getSolutions()).extracting(SolutionProposee::getTempsEstime).containsExactly(null, null);
        assertThat(reponse.getSolutions()).extracting(SolutionProposee::getIdAssigne).containsExactly(null, null);
    }

    @Test
    void interpreter_typeInconnu_estTraiteCommeUneTacheDeSuivi() {
        String solution = "{\"libelle\":\"Faire autrement\",\"justification\":\"j\",\"gain\":\"g\",\"typeAction\":\"AUTRE\"}";

        assertThat(service.interpreter(solutions(solution), membres, taches).getSolutions().get(0).getType())
                .isEqualTo(TypeActionRecommandation.CREER_TACHE);
    }

    @Test
    void interpreter_libelleVideOuDoublon_ecarteCesSolutions() {
        ReponseSuggestionRisque reponse = service.interpreter(solutions(
                creation("   ", ""),
                creation("Renforcer le suivi hebdomadaire", ""),
                creation("renforcer le suivi hebdomadaire", "")), membres, taches);

        assertThat(reponse.getSolutions()).extracting(SolutionProposee::getLibelle)
                .containsExactly("Renforcer le suivi hebdomadaire");
    }

    @Test
    void interpreter_plusDeSolutionsQueLePlafond_neGardeQueLePlafond() {
        String[] libelles = {"Alpha bravo charlie", "Delta echo foxtrot", "Golf hotel india",
                "Juliet kilo lima", "Mike november oscar"};
        String[] brutes = new String[libelles.length];
        for (int i = 0; i < libelles.length; i++) {
            brutes[i] = creation(libelles[i], "");
        }

        assertThat(service.interpreter(solutions(brutes), membres, taches).getSolutions()).hasSize(4);
    }

    @Test
    void interpreter_champsSansTitreNiDescription_utiliseLibelleEtJustification() {
        SolutionProposee solution = service.interpreter(solutions(creation("Sécuriser la recette", "")), membres, taches)
                .getSolutions().get(0);

        assertThat(solution.getTitreTache()).isEqualTo("Sécuriser la recette");
        assertThat(solution.getDescriptionTache()).isEqualTo("Parce que");
    }

    @Test
    void interpreter_libelleTropLong_estTronque() {
        StringBuilder libelle = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            libelle.append('x');
        }

        SolutionProposee solution = service.interpreter(solutions(creation(libelle.toString(), "")), membres, taches)
                .getSolutions().get(0);

        assertThat(solution.getLibelle()).hasSize(300).endsWith("…");
        assertThat(solution.getTitreTache()).hasSize(200).endsWith("…");
    }

    @Test
    void interpreter_reponseInexploitable_leveReponseIaInvalideException() {
        assertThatThrownBy(() -> service.interpreter(null, membres, taches)).isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter("pas du json {", membres, taches)).isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter("[]", membres, taches)).isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter("{\"autre\":1}", membres, taches)).isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter("{\"solutions\":\"aucune\"}", membres, taches)).isInstanceOf(ReponseIaInvalideException.class);
    }

    @Test
    void interpreter_aucuneSolutionExploitable_leveReponseIaInvalideException() {
        assertThatThrownBy(() -> service.interpreter("{\"solutions\":[]}", membres, taches))
                .isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter(solutions(creation("", "")), membres, taches))
                .isInstanceOf(ReponseIaInvalideException.class);
    }
}
