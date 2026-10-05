package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutRisque;

public class StatutRisqueInvalideException extends RuntimeException {

    public StatutRisqueInvalideException(Long idRisque, StatutRisque statutActuel, String actionDemandee) {
        super("Le risque " + idRisque + " ne peut pas " + actionDemandee
                + " depuis le statut " + statutActuel + ".");
    }
}
