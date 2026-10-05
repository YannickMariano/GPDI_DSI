package mg.gov.mef.dsi.gpdi.exception;

public class UtilisateurIntrouvableException extends RuntimeException {

    public UtilisateurIntrouvableException(Long id) {
        super("L'utilisateur " + id + " est introuvable.");
    }
}
