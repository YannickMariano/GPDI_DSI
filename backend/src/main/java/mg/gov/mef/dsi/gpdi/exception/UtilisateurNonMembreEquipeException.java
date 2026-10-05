package mg.gov.mef.dsi.gpdi.exception;

public class UtilisateurNonMembreEquipeException extends RuntimeException {

    public UtilisateurNonMembreEquipeException(Long idUtilisateur) {
        super("L'utilisateur " + idUtilisateur
                + " n'est pas membre de l'equipe du projet et ne peut pas etre assigne a cette tache.");
    }
}
