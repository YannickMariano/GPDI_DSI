package mg.gov.mef.dsi.gpdi.exception;

/** Plusieurs solutions cochees en meme temps proposent de reaffecter la meme tache a des agents differents. */
public class RecommandationsConflictuellesException extends RuntimeException {

    public RecommandationsConflictuellesException(String codeTache) {
        super("Plusieurs solutions sélectionnées réaffectent la tâche " + codeTache
                + " à des agents différents : ne cochez qu'une seule de ces solutions.");
    }
}
