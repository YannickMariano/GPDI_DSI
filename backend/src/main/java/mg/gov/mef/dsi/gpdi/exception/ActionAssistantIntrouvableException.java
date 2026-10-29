package mg.gov.mef.dsi.gpdi.exception;

/** Action inexistante, ou proposee a un autre utilisateur que l'utilisateur courant. */
public class ActionAssistantIntrouvableException extends RuntimeException {

    public ActionAssistantIntrouvableException(Long idAction) {
        super("Action de l'assistant introuvable : " + idAction + ".");
    }
}
