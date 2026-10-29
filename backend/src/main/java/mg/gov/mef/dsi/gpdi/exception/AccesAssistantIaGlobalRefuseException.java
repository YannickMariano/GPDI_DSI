package mg.gov.mef.dsi.gpdi.exception;

/** L'utilisateur courant n'est pas chef de projet : l'assistant IA sur l'ensemble de ses projets ne lui est pas accessible. */
public class AccesAssistantIaGlobalRefuseException extends RuntimeException {

    public AccesAssistantIaGlobalRefuseException() {
        super("L'assistant IA est réservé aux chefs de projet.");
    }
}
