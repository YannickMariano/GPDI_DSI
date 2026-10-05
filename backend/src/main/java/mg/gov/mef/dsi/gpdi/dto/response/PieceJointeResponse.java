package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PieceJointeResponse {

    private Long id;

    private String nomFichier;

    private String typeContenu;

    private Long taille;
}
