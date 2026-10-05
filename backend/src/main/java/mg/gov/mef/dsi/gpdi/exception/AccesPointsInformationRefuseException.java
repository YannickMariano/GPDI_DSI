package mg.gov.mef.dsi.gpdi.exception;

/**
 * L'utilisateur courant n'est ni le chef de projet, ni un compte client avec un partage actif sur ce
 * projet : il n'a pas le droit de publier ou de consulter les points d'information de ce projet.
 */
public class AccesPointsInformationRefuseException extends RuntimeException {

    public AccesPointsInformationRefuseException(Long idProjet) {
        super("Vous n'avez pas accès aux points d'information du projet " + idProjet + ".");
    }
}
