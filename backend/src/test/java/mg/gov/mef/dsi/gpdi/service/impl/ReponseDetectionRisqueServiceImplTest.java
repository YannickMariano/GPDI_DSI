package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReponseDetectionRisqueServiceImplTest {

    private ReponseDetectionRisqueServiceImpl service;
    private ContexteProjetIa contexte;

    @BeforeEach
    void setUp() {
        service = new ReponseDetectionRisqueServiceImpl(new ObjectMapper());
        contexte = new ContexteProjetIa(
                new ObjectMapper().createObjectNode(),
                new HashSet<>(Arrays.asList(10L, 11L)),
                new HashSet<>(Collections.singletonList(5L)),
                Collections.singletonList("Retard de livraison du référentiel fournisseurs"));
    }

    private static String risque(String libelle, String probabilite, String impact, String idPorteur) {
        return "{\"libelle\":" + libelle + ",\"description\":\"desc\",\"probabilite\":" + probabilite
                + ",\"impact\":" + impact + ",\"parade\":\"parade\",\"idPorteur\":" + idPorteur + "}";
    }

    private static String reponse(String risques, String reevaluations, String confiance) {
        return "{\"nouveauxRisques\":[" + risques + "],\"reevaluations\":[" + reevaluations
                + "],\"synthese\":\"Synthese\",\"indiceConfiance\":" + confiance + "}";
    }

    @Test
    void interpreter_reponseValide_retourneLesElementsEtLaSynthese() {
        String json = reponse(risque("\"Sous-effectif pendant les congés\"", "4", "3", "10"),
                "{\"idRisque\":5,\"probabilite\":2,\"impact\":2}", "0.8");

        ReponseDetectionRisque resultat = service.interpreter(json, contexte);

        assertThat(resultat.getNouveauxRisques()).hasSize(1);
        ReponseDetectionRisque.RisqueDetecte risque = resultat.getNouveauxRisques().get(0);
        assertThat(risque.getLibelle()).isEqualTo("Sous-effectif pendant les congés");
        assertThat(risque.getProbabilite()).isEqualTo(4);
        assertThat(risque.getImpact()).isEqualTo(3);
        assertThat(risque.getParade()).isEqualTo("parade");
        assertThat(risque.getIdPorteur()).isEqualTo(10L);
        assertThat(resultat.getReevaluations()).hasSize(1);
        assertThat(resultat.getReevaluations().get(0).getIdRisque()).isEqualTo(5L);
        assertThat(resultat.getSynthese()).isEqualTo("Synthese");
        assertThat(resultat.getIndiceConfiance()).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    void interpreter_notesHorsEchelleOuLibelleVide_ecarteCesRisquesSeulement() {
        String json = reponse(String.join(",",
                risque("\"Risque A\"", "6", "3", "null"),
                risque("\"Risque B\"", "3", "0", "null"),
                risque("\"Risque C\"", "2.5", "3", "null"),
                risque("\"   \"", "3", "3", "null"),
                risque("\"Dépendance envers un prestataire\"", "3", "4", "null")), "", "null");

        ReponseDetectionRisque resultat = service.interpreter(json, contexte);

        assertThat(resultat.getNouveauxRisques()).extracting(ReponseDetectionRisque.RisqueDetecte::getLibelle)
                .containsExactly("Dépendance envers un prestataire");
    }

    @Test
    void interpreter_doublonDUnRisqueConnu_leEcarte() {
        String json = reponse(risque("\"Retard de livraison du référentiel fournisseurs par le prestataire\"", "3", "3", "null"),
                "", "null");

        assertThat(service.interpreter(json, contexte).getNouveauxRisques()).isEmpty();
    }

    @Test
    void interpreter_doublonDansLaMemeReponse_neGardeQueLePremier() {
        String json = reponse(String.join(",",
                risque("\"Sous-effectif pendant les congés\"", "4", "3", "null"),
                risque("\"sous effectif pendant les conges\"", "4", "3", "null")), "", "null");

        assertThat(service.interpreter(json, contexte).getNouveauxRisques()).hasSize(1);
    }

    @Test
    void interpreter_plusDeRisquesQueLePlafond_neGardeQueLePlafond() {
        String[] libelles = {"Alpha bravo charlie", "Delta echo foxtrot", "Golf hotel india",
                "Juliet kilo lima", "Mike november oscar", "Papa quebec romeo", "Sierra tango uniform"};
        StringBuilder risques = new StringBuilder();
        for (String libelle : libelles) {
            if (risques.length() > 0) {
                risques.append(',');
            }
            risques.append(risque("\"" + libelle + "\"", "3", "3", "null"));
        }

        assertThat(service.interpreter(reponse(risques.toString(), "", "null"), contexte).getNouveauxRisques())
                .hasSize(5);
    }

    @Test
    void interpreter_porteurInconnuOuAbsent_metNull() {
        String json = reponse(String.join(",",
                risque("\"Alpha bravo charlie\"", "3", "3", "999"),
                risque("\"Delta echo foxtrot\"", "3", "3", "null"),
                risque("\"Golf hotel india\"", "3", "3", "\"10\"")), "", "null");

        assertThat(service.interpreter(json, contexte).getNouveauxRisques())
                .extracting(ReponseDetectionRisque.RisqueDetecte::getIdPorteur)
                .containsExactly(null, null, null);
    }

    @Test
    void interpreter_reevaluationsInvalides_neGardeQueLaPremiereValide() {
        String json = reponse("", String.join(",",
                "{\"idRisque\":99,\"probabilite\":2,\"impact\":2}",
                "{\"idRisque\":5,\"probabilite\":9,\"impact\":2}",
                "{\"idRisque\":5,\"probabilite\":2,\"impact\":1}",
                "{\"idRisque\":5,\"probabilite\":1,\"impact\":1}"), "null");

        ReponseDetectionRisque resultat = service.interpreter(json, contexte);

        assertThat(resultat.getReevaluations()).hasSize(1);
        assertThat(resultat.getReevaluations().get(0).getProbabilite()).isEqualTo(2);
        assertThat(resultat.getReevaluations().get(0).getImpact()).isEqualTo(1);
    }

    @Test
    void interpreter_indiceConfianceHorsBornesOuAbsent_metNull() {
        assertThat(service.interpreter(reponse("", "", "2.5"), contexte).getIndiceConfiance()).isNull();
        assertThat(service.interpreter(reponse("", "", "-0.1"), contexte).getIndiceConfiance()).isNull();
        assertThat(service.interpreter(reponse("", "", "null"), contexte).getIndiceConfiance()).isNull();
        assertThat(service.interpreter(reponse("", "", "\"0.9\""), contexte).getIndiceConfiance()).isNull();
        assertThat(service.interpreter(reponse("", "", "1"), contexte).getIndiceConfiance()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void interpreter_textesTropLongs_sontTronques() {
        StringBuilder libelleLong = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            libelleLong.append('x');
        }

        ReponseDetectionRisque resultat = service.interpreter(
                reponse(risque("\"" + libelleLong + "\"", "3", "3", "null"), "", "null"), contexte);

        String libelle = resultat.getNouveauxRisques().get(0).getLibelle();
        assertThat(libelle).hasSize(300).endsWith("…");
    }

    @Test
    void interpreter_jsonInvalide_leveReponseIaInvalideException() {
        assertThatThrownBy(() -> service.interpreter("pas du json {", contexte))
                .isInstanceOf(ReponseIaInvalideException.class);
    }

    @Test
    void interpreter_reponseVideOuNull_leveReponseIaInvalideException() {
        assertThatThrownBy(() -> service.interpreter("  ", contexte)).isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter(null, contexte)).isInstanceOf(ReponseIaInvalideException.class);
    }

    @Test
    void interpreter_jsonNonObjetOuSansListeDeRisques_leveReponseIaInvalideException() {
        assertThatThrownBy(() -> service.interpreter("[1,2]", contexte)).isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter("{\"synthese\":\"x\"}", contexte)).isInstanceOf(ReponseIaInvalideException.class);
        assertThatThrownBy(() -> service.interpreter("{\"nouveauxRisques\":\"aucun\"}", contexte)).isInstanceOf(ReponseIaInvalideException.class);
    }

    @Test
    void interpreter_listeDeRisquesVideSansReevaluations_estAcceptee() {
        ReponseDetectionRisque resultat = service.interpreter("{\"nouveauxRisques\":[]}", contexte);

        assertThat(resultat.getNouveauxRisques()).isEmpty();
        assertThat(resultat.getReevaluations()).isEmpty();
        assertThat(resultat.getSynthese()).isNull();
        assertThat(resultat.getIndiceConfiance()).isNull();
    }
}
