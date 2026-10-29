package mg.gov.mef.dsi.gpdi.exception;

/** Le statut d'une tâche archivée est figé : il faut d'abord la restaurer. */
public class StatutTacheArchiveeException extends RuntimeException {

    public StatutTacheArchiveeException(String codeTache) {
        super("La tâche " + codeTache + " est archivée : restaurez-la avant de changer son statut.");
    }
}
