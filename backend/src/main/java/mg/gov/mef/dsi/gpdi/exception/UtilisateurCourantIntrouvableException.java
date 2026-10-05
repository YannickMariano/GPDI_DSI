package mg.gov.mef.dsi.gpdi.exception;

public class UtilisateurCourantIntrouvableException extends RuntimeException {

    public UtilisateurCourantIntrouvableException(String matricule) {
        super("Aucun utilisateur ne correspond au compte connecte (matricule " + matricule + ").");
    }
}
