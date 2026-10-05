package mg.gov.mef.dsi.gpdi.exception;

/** L'utilisateur courant n'est pas le chef de projet (responsable) de ce projet : l'assistant IA ne lui est pas accessible. */
public class AccesAssistantIaRefuseException extends RuntimeException {

    public AccesAssistantIaRefuseException(Long idProjet) {
        super("Vous n'avez pas accès à l'assistant IA du projet " + idProjet + ".");
    }
}
