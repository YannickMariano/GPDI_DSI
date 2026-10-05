package mg.gov.mef.dsi.gpdi.exception;

import java.time.LocalDate;

public class SprintChevauchementException extends RuntimeException {

    public SprintChevauchementException(LocalDate dateDebut, LocalDate dateFin) {
        super("Les dates du " + dateDebut + " au " + dateFin
                + " chevauchent un autre sprint de ce projet.");
    }
}
