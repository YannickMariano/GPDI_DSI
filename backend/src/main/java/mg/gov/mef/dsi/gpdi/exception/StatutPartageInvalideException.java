package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutPartage;

public class StatutPartageInvalideException extends RuntimeException {

    public StatutPartageInvalideException(Long idPartage, StatutPartage statutActuel) {
        super("Le partage " + idPartage + " ne peut pas etre revoque depuis le statut " + statutActuel + ".");
    }
}
