package mg.gov.mef.dsi.gpdi.exception;

public class AnalyseDejaEnCoursException extends RuntimeException {

    public AnalyseDejaEnCoursException(Long idProjet) {
        super("Une analyse des risques est déjà en cours pour le projet " + idProjet
                + " ; patientez quelques instants puis actualisez la liste des risques.");
    }
}
