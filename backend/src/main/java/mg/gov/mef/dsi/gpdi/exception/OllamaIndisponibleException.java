package mg.gov.mef.dsi.gpdi.exception;

public class OllamaIndisponibleException extends RuntimeException {

    public OllamaIndisponibleException(String message) {
        super(message);
    }

    public OllamaIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
