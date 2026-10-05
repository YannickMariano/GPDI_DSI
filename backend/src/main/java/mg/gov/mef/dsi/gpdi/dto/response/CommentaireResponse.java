package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class CommentaireResponse {

    private Long id;

    private Long idAuteur;

    private String nomAuteur;

    private String contenu;

    private LocalDateTime dateCreation;

    /** Renseignee uniquement si le commentaire a une piece jointe (voir PieceJointeCommentaire). */
    private PieceJointeResponse pieceJointe;
}
