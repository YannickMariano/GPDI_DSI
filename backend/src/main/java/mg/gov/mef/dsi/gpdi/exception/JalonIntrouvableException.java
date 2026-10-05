package mg.gov.mef.dsi.gpdi.exception;

public class JalonIntrouvableException extends RuntimeException {

    public JalonIntrouvableException(Long id) {
        super("Le jalon " + id + " est introuvable.");
    }
}
