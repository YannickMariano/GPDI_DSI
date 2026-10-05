package mg.gov.mef.dsi.gpdi.exception;

public class UtilisateurNonEligibleException extends RuntimeException {

    public UtilisateurNonEligibleException(Long idUtilisateur, String rolesRequis) {
        super("L'utilisateur " + idUtilisateur
                + " n'a pas un role autorise pour cette operation (role(s) requis : " + rolesRequis + ").");
    }
}
