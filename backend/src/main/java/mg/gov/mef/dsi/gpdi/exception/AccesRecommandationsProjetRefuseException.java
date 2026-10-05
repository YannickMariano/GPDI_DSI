package mg.gov.mef.dsi.gpdi.exception;

/** L'utilisateur courant n'est pas le chef de projet (responsable) de ce projet : les recommandations de pilotage ne lui sont pas accessibles. */
public class AccesRecommandationsProjetRefuseException extends RuntimeException {

    public AccesRecommandationsProjetRefuseException(Long idProjet) {
        super("Vous n'avez pas accès aux recommandations de pilotage du projet " + idProjet + ".");
    }
}
