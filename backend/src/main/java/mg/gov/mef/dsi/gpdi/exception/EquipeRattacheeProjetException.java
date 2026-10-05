package mg.gov.mef.dsi.gpdi.exception;

public class EquipeRattacheeProjetException extends RuntimeException {

    public EquipeRattacheeProjetException(Long idEquipe) {
        super("L'equipe " + idEquipe + " ne peut pas etre supprimee car elle est rattachee a un projet.");
    }
}
