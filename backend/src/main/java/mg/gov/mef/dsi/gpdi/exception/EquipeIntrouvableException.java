package mg.gov.mef.dsi.gpdi.exception;

public class EquipeIntrouvableException extends RuntimeException {

    public EquipeIntrouvableException(Long id) {
        super("L'equipe " + id + " est introuvable.");
    }
}
