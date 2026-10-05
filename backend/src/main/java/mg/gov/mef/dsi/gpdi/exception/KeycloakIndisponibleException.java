package mg.gov.mef.dsi.gpdi.exception;

public class KeycloakIndisponibleException extends RuntimeException {

    public KeycloakIndisponibleException(String message) {
        super(message);
    }

    public KeycloakIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
