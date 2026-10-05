package mg.gov.mef.dsi.gpdi.exception;

import java.time.LocalDate;

public class DateFinTacheInvalideException extends RuntimeException {

    public DateFinTacheInvalideException(LocalDate dateFinReelle, LocalDate dateFinSprint) {
        super("La date de cloture de la tache (" + dateFinReelle
                + ") ne peut pas depasser la date de cloture du sprint (" + dateFinSprint + ").");
    }
}
