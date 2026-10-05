package mg.gov.mef.dsi.gpdi.exception;

public class SolutionRisqueIntrouvableException extends RuntimeException {

    public SolutionRisqueIntrouvableException(Long idSolution, Long idRisque) {
        super("La solution " + idSolution + " n'existe pas pour le risque " + idRisque + ".");
    }
}
