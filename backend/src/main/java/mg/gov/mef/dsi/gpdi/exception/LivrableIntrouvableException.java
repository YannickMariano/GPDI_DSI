package mg.gov.mef.dsi.gpdi.exception;

public class LivrableIntrouvableException extends RuntimeException {

    public LivrableIntrouvableException(Long id) {
        super("Le livrable " + id + " est introuvable.");
    }
}
