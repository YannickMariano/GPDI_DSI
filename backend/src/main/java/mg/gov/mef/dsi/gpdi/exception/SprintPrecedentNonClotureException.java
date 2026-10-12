package mg.gov.mef.dsi.gpdi.exception;

public class SprintPrecedentNonClotureException extends RuntimeException {

    public SprintPrecedentNonClotureException(String nomSprintPrecedent) {
        super("Ce sprint ne peut pas être démarré : le sprint précédent (" + nomSprintPrecedent
                + ") doit d'abord être clôturé.");
    }
}
