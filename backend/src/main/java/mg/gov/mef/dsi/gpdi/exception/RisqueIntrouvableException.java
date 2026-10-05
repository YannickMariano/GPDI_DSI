package mg.gov.mef.dsi.gpdi.exception;

public class RisqueIntrouvableException extends RuntimeException {

    public RisqueIntrouvableException(Long id) {
        super("Le risque " + id + " est introuvable.");
    }
}
