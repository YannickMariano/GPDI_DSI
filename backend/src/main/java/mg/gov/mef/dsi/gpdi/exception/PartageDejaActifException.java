package mg.gov.mef.dsi.gpdi.exception;

public class PartageDejaActifException extends RuntimeException {

    public PartageDejaActifException(Long idUtilisateur, Long idProjet) {
        super("Le projet " + idProjet + " est deja partage activement avec le client " + idUtilisateur
                + " ; revoquez ou attendez l'echeance du partage existant avant d'en creer un nouveau.");
    }
}
