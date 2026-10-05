package mg.gov.mef.dsi.gpdi.exception;

public class AnalyseRecommandationsDejaEnCoursException extends RuntimeException {

    public AnalyseRecommandationsDejaEnCoursException(Long idProjet) {
        super("Une analyse des recommandations est déjà en cours pour le projet " + idProjet
                + " ; patientez quelques instants puis réessayez.");
    }
}
