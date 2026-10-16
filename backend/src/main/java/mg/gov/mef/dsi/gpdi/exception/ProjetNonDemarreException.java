package mg.gov.mef.dsi.gpdi.exception;

/** Leve quand une action (demarrer un sprint, cloturer un jalon...) est demandee alors que le projet est encore en preparation. */
public class ProjetNonDemarreException extends RuntimeException {

    public ProjetNonDemarreException(String actionDemandee) {
        super("Impossible de " + actionDemandee + " : le projet n'est pas encore démarré (statut « En préparation »).");
    }
}
