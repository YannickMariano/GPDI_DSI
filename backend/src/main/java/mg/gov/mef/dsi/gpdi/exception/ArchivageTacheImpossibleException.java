package mg.gov.mef.dsi.gpdi.exception;

/** Seule une tâche terminée, et pas encore archivée, peut être archivée. */
public class ArchivageTacheImpossibleException extends RuntimeException {

    public ArchivageTacheImpossibleException(String message) {
        super(message);
    }
}
