package mg.gov.mef.dsi.gpdi.exception;

public class TachePrecedenceDejaExistanteException extends RuntimeException {

    public TachePrecedenceDejaExistanteException(Long idTacheSource, Long idTacheCible) {
        super("Une relation existe deja entre la tache " + idTacheSource + " et la tache " + idTacheCible + ".");
    }
}
