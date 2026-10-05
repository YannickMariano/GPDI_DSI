package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.CommentaireRequest;
import mg.gov.mef.dsi.gpdi.dto.response.CommentaireResponse;
import mg.gov.mef.dsi.gpdi.entity.PieceJointeCommentaire;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommentaireService {

    CommentaireResponse creer(Long idTache, Long idAuteur, CommentaireRequest request);

    List<CommentaireResponse> rechercher(Long idTache);

    /** `contenu` est facultatif : une piece jointe seule, sans texte, est un commentaire valide. */
    CommentaireResponse creerAvecPieceJointe(Long idTache, Long idAuteur, String contenu, MultipartFile fichier);

    PieceJointeCommentaire telechargerPieceJointe(Long idTache, Long idCommentaire);
}
