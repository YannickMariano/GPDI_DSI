package mg.gov.mef.dsi.gpdi.exception;

public class NotificationIntrouvableException extends RuntimeException {

    public NotificationIntrouvableException(Long id) {
        super("Notification introuvable : " + id);
    }
}
