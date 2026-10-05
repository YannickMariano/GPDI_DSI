package mg.gov.mef.dsi.gpdi.exception;

public class CompteSuspenduException extends RuntimeException {

    public CompteSuspenduException() {
        super("Votre compte a été suspendu. Demandez à votre supérieur de réactiver votre compte.");
    }
}
