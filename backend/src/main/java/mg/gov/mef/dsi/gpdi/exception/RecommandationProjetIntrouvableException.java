package mg.gov.mef.dsi.gpdi.exception;

public class RecommandationProjetIntrouvableException extends RuntimeException {

    public RecommandationProjetIntrouvableException(Long idRecommandation, Long idProjet) {
        super("La recommandation " + idRecommandation + " n'existe pas pour le projet " + idProjet + ".");
    }
}
