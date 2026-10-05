package mg.gov.mef.dsi.gpdi.entity;

public enum NiveauRisque {
    FAIBLE,
    MOYEN,
    ELEVE,
    CRITIQUE;

    private static final int SEUIL_CRITIQUE = 15;
    private static final int SEUIL_ELEVE = 10;
    private static final int SEUIL_MOYEN = 5;

    /** Niveau d'un risque a partir de probabilite x impact (chacun de 1 a 5, soit un score de 1 a 25). */
    public static NiveauRisque calculer(int probabilite, int impact) {
        int score = probabilite * impact;
        if (score >= SEUIL_CRITIQUE) {
            return CRITIQUE;
        }
        if (score >= SEUIL_ELEVE) {
            return ELEVE;
        }
        return score >= SEUIL_MOYEN ? MOYEN : FAIBLE;
    }
}
