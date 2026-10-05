package mg.gov.mef.dsi.gpdi.exception;

public class RoleIntrouvableException extends RuntimeException {

    public RoleIntrouvableException(Long id) {
        super("Le role " + id + " est introuvable.");
    }
}