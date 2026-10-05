package mg.gov.mef.dsi.gpdi.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimilariteTexteTest {

    @Test
    void similaires_memeLibelleAvecAccentsEtCasseDifferents_retourneVrai() {
        assertThat(SimilariteTexte.similaires("Dépendance à une personne clé", "DEPENDANCE A UNE PERSONNE CLE")).isTrue();
    }

    @Test
    void similaires_libelleReformuleAvecMotsAjoutes_retourneVrai() {
        assertThat(SimilariteTexte.similaires(
                "Retard de livraison du référentiel fournisseurs",
                "Retard de livraison du référentiel fournisseurs par le prestataire")).isTrue();
    }

    @Test
    void similaires_sujetsDifferents_retourneFaux() {
        assertThat(SimilariteTexte.similaires(
                "Retard de livraison du référentiel fournisseurs",
                "Non-conformité RGPD des données migrées")).isFalse();
    }

    @Test
    void similaires_unSeulMotEnCommun_retourneFaux() {
        assertThat(SimilariteTexte.similaires("Retard du référentiel", "Incompatibilité du référentiel avec le SI")).isFalse();
    }

    @Test
    void similaires_libelleSansMotSignificatifIdentique_retourneVrai() {
        assertThat(SimilariteTexte.similaires("Le", "le")).isTrue();
    }

    @Test
    void similaires_libelleNull_retourneFaux() {
        assertThat(SimilariteTexte.similaires(null, "Retard")).isFalse();
    }

    @Test
    void jetons_texteAvecMotsVidesEtPonctuation_neGardeQueLesMotsSignificatifs() {
        assertThat(SimilariteTexte.jetons("Sous-effectif pendant les congés d'été !"))
                .containsExactlyInAnyOrder("sous", "effectif", "pendant", "conges", "ete");
    }
}
