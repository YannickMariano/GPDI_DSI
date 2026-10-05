package mg.gov.mef.dsi.gpdi.exception;

public class UtilisateurAssigneATachesException extends RuntimeException {

    public UtilisateurAssigneATachesException(Long idUtilisateur) {
        super("L'utilisateur " + idUtilisateur + " ne peut pas etre supprime car il est encore assigne a des taches.");
    }
}
