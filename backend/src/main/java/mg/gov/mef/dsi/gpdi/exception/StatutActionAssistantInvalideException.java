package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutActionAssistant;

/** Une action deja confirmee ou annulee ne peut plus etre traitee. */
public class StatutActionAssistantInvalideException extends RuntimeException {

    public StatutActionAssistantInvalideException(Long idAction, StatutActionAssistant statut) {
        super("L'action " + idAction + " a déjà été traitée (" + (statut == StatutActionAssistant.APPLIQUEE ? "confirmée" : "annulée") + ").");
    }
}
