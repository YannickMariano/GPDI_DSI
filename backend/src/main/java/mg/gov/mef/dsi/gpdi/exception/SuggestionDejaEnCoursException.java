package mg.gov.mef.dsi.gpdi.exception;

public class SuggestionDejaEnCoursException extends RuntimeException {

    public SuggestionDejaEnCoursException(Long idRisque) {
        super("Des solutions sont déjà en cours de recherche pour le risque " + idRisque
                + " ; patientez quelques instants puis relancez la demande.");
    }
}
