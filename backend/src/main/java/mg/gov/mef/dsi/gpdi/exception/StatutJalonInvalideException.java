package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutJalon;

public class StatutJalonInvalideException extends RuntimeException {

    public StatutJalonInvalideException(Long idJalon, StatutJalon statutActuel, String actionDemandee) {
        super("Le jalon " + idJalon + " ne peut pas etre " + actionDemandee
                + " depuis le statut " + statutActuel + ".");
    }
}
