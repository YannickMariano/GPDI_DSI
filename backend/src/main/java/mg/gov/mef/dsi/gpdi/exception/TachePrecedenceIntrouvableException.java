package mg.gov.mef.dsi.gpdi.exception;

public class TachePrecedenceIntrouvableException extends RuntimeException {

    public TachePrecedenceIntrouvableException(Long idTacheSource, Long idTacheCible) {
        super("Aucune relation trouvee entre la tache " + idTacheSource + " et la tache " + idTacheCible + ".");
    }
}
