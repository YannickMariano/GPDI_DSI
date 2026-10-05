package mg.gov.mef.dsi.gpdi.exception;

import mg.gov.mef.dsi.gpdi.entity.StatutProjet;

public class StatutProjetInvalideException extends RuntimeException {

    public StatutProjetInvalideException(StatutProjet statut) {
        super("Le statut " + statut
                + " ne peut pas etre defini directement ; utilisez les actions dediees (cloturer/archiver).");
    }
}
