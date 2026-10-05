package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;

public class StatutRecommandationInvalideException extends RuntimeException {

    public StatutRecommandationInvalideException(Long idSolution, StatutRecommandation statutActuel) {
        super("La solution " + idSolution + " ne peut pas être appliquée : elle est déjà au statut " + statutActuel + ".");
    }
}
