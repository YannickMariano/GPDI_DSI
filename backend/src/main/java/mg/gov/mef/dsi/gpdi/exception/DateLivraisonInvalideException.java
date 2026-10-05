package mg.gov.mef.dsi.gpdi.exception;

import java.time.LocalDate;

public class DateLivraisonInvalideException extends RuntimeException {

    public DateLivraisonInvalideException(LocalDate dateLivraison, LocalDate dateEcheanceJalon) {
        super("La date de livraison (" + dateLivraison
                + ") ne peut pas depasser la date d'echeance du jalon (" + dateEcheanceJalon + ").");
    }
}
