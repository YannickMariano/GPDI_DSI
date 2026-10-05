package mg.gov.mef.dsi.gpdi.exception;

public class PartageClientIntrouvableException extends RuntimeException {

    public PartageClientIntrouvableException(Long id) {
        super("Le partage " + id + " est introuvable.");
    }
}
