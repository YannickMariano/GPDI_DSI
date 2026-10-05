package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;

public class StatutRecommandationProjetInvalideException extends RuntimeException {

    public StatutRecommandationProjetInvalideException(Long idRecommandation, StatutRecommandation statutActuel) {
        super("La recommandation " + idRecommandation + " ne peut pas être traitée : elle est déjà au statut " + statutActuel + ".");
    }
}
