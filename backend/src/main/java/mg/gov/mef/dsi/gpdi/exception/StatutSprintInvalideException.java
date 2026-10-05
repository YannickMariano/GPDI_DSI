package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutSprint;

public class StatutSprintInvalideException extends RuntimeException {

    public StatutSprintInvalideException(Long idSprint, StatutSprint statutActuel, String actionDemandee) {
        super("Le sprint " + idSprint + " ne peut pas etre " + actionDemandee
                + " depuis le statut " + statutActuel + ".");
    }
}
