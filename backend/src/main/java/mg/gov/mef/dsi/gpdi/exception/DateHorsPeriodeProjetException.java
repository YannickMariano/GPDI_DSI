package mg.gov.mef.dsi.gpdi.exception;

import java.time.LocalDate;

/** Leve quand la date (debut, fin ou echeance) d'un sprint ou d'un jalon depasse la date de fin de son projet. */
public class DateHorsPeriodeProjetException extends RuntimeException {

    public DateHorsPeriodeProjetException(LocalDate dateFinProjet) {
        super("La date ne peut pas dépasser le " + dateFinProjet + ", date de fin du projet.");
    }
}
