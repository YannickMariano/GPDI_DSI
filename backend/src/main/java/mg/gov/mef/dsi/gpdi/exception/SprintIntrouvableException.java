package mg.gov.mef.dsi.gpdi.exception;

public class SprintIntrouvableException extends RuntimeException {

    public SprintIntrouvableException(Long id) {
        super("Le sprint " + id + " est introuvable.");
    }
}
