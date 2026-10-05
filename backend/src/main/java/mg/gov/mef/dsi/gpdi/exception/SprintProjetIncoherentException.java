package mg.gov.mef.dsi.gpdi.exception;

public class SprintProjetIncoherentException extends RuntimeException {

    public SprintProjetIncoherentException(Long idSprint, Long idProjet) {
        super("Le sprint " + idSprint + " n'appartient pas au projet " + idProjet + ".");
    }
}
