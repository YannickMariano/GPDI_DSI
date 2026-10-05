package mg.gov.mef.dsi.gpdi.exception;

public class SousTacheIntrouvableException extends RuntimeException {

    public SousTacheIntrouvableException(Long id) {
        super("La sous-tache " + id + " est introuvable.");
    }
}
