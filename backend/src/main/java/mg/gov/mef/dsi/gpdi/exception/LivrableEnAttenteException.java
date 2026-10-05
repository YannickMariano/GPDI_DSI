package mg.gov.mef.dsi.gpdi.exception;

public class LivrableEnAttenteException extends RuntimeException {

    public LivrableEnAttenteException(Long idJalon) {
        super("Impossible de terminer : un livrable est encore en attente pour ce jalon.");
    }
}
