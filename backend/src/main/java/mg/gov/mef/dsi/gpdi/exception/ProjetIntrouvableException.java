package mg.gov.mef.dsi.gpdi.exception;

public class ProjetIntrouvableException extends RuntimeException {

    public ProjetIntrouvableException(Long id) {
        super("Le projet " + id + " est introuvable.");
    }
}
