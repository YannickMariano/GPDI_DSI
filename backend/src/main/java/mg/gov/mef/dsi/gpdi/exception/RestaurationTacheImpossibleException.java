package mg.gov.mef.dsi.gpdi.exception;

/** Seule une tâche archivée peut être restaurée. */
public class RestaurationTacheImpossibleException extends RuntimeException {

    public RestaurationTacheImpossibleException(String codeTache) {
        super("La tâche " + codeTache + " n'est pas archivée : elle ne peut pas être restaurée.");
    }
}
