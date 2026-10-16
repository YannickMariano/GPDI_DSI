package mg.gov.mef.dsi.gpdi.exception;

/** Leve quand une action est demandee sur un jalon annule, ou sur l'un des livrables qui lui sont lies. */
public class JalonAnnuleException extends RuntimeException {

    public JalonAnnuleException(Long idJalon) {
        super("Le jalon " + idJalon + " est annulé : aucune action n'est plus possible sur ce jalon ni sur ses livrables.");
    }
}
