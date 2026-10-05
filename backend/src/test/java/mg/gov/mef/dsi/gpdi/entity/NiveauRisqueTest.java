package mg.gov.mef.dsi.gpdi.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NiveauRisqueTest {

    @Test
    void calculer_scoreParPalier_retourneLeNiveauAttendu() {
        assertThat(NiveauRisque.calculer(1, 1)).isEqualTo(NiveauRisque.FAIBLE);
        assertThat(NiveauRisque.calculer(2, 2)).isEqualTo(NiveauRisque.FAIBLE);
        assertThat(NiveauRisque.calculer(1, 5)).isEqualTo(NiveauRisque.MOYEN);
        assertThat(NiveauRisque.calculer(3, 3)).isEqualTo(NiveauRisque.MOYEN);
        assertThat(NiveauRisque.calculer(2, 5)).isEqualTo(NiveauRisque.ELEVE);
        assertThat(NiveauRisque.calculer(3, 4)).isEqualTo(NiveauRisque.ELEVE);
        assertThat(NiveauRisque.calculer(3, 5)).isEqualTo(NiveauRisque.CRITIQUE);
        assertThat(NiveauRisque.calculer(5, 5)).isEqualTo(NiveauRisque.CRITIQUE);
    }
}
