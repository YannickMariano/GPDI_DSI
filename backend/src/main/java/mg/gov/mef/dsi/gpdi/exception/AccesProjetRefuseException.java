package mg.gov.mef.dsi.gpdi.exception;

/**
 * L'utilisateur courant n'est ni le responsable, ni le chef d'equipe, ni un membre de l'equipe de ce
 * projet (et, pour un acces en lecture, ni le Directeur DSI, ni un compte client avec un partage actif).
 */
public class AccesProjetRefuseException extends RuntimeException {

    public AccesProjetRefuseException(Long idProjet) {
        super("Vous n'avez pas accès au projet " + idProjet + ".");
    }
}
