package mg.gov.mef.dsi.gpdi.exception;

/** Leve quand un projet est cloture alors qu'il reste au moins un sprint ou un jalon non termine. */
public class ClotureProjetImpossibleException extends RuntimeException {

    public ClotureProjetImpossibleException() {
        super("Clôture impossible : un sprint est encore en cours ou un jalon est encore en cours.");
    }
}
