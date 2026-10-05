package mg.gov.mef.dsi.gpdi.exception;

public class PieceJointeIntrouvableException extends RuntimeException {

    public PieceJointeIntrouvableException(Long idCommentaire) {
        super("Le commentaire " + idCommentaire + " n'a pas de piece jointe.");
    }
}
