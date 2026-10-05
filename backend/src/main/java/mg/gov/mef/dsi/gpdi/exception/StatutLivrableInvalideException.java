package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;

public class StatutLivrableInvalideException extends RuntimeException {

    public StatutLivrableInvalideException(Long idLivrable, StatutLivrable statutActuel, String actionDemandee) {
        super("Le livrable " + idLivrable + " ne peut pas etre " + actionDemandee
                + " depuis le statut " + statutActuel + ".");
    }
}
