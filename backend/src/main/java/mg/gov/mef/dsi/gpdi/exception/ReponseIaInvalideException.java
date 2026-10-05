package mg.gov.mef.dsi.gpdi.exception;

public class ReponseIaInvalideException extends RuntimeException {

    public ReponseIaInvalideException(String message) {
        super(message);
    }

    public ReponseIaInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
