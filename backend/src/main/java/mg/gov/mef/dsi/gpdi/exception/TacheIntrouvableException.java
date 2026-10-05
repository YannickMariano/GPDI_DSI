package mg.gov.mef.dsi.gpdi.exception;

public class TacheIntrouvableException extends RuntimeException {

    public TacheIntrouvableException(Long id) {
        super("La tache " + id + " est introuvable.");
    }
}
