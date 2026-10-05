package mg.gov.mef.dsi.gpdi.exception;

public class HabilitationRefuseeException extends RuntimeException {

    public HabilitationRefuseeException(String fonction) {
        super("Votre role ne dispose pas du droit d'ecriture sur la fonction « " + fonction + " ».");
    }
}
