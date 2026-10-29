package mg.gov.mef.dsi.gpdi.exception;

/**
 * L'utilisateur n'a pas le role Keycloak qui protege la route HTTP equivalente a l'action (ex. GESTIONNAIRE_SPRINT
 * pour creer un sprint) : l'assistant ne permet pas de contourner ce premier niveau de controle d'acces.
 */
public class RoleActionAssistantManquantException extends RuntimeException {

    public RoleActionAssistantManquantException(String role) {
        super("Accès refusé : votre compte ne dispose pas du rôle " + role + " nécessaire pour cette action.");
    }
}
