package mg.gov.mef.dsi.gpdi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RapportDiffuseResponse {

    private Long id;

    private Long idProjet;

    private String nomProjet;

    private String modele;

    private String nom;

    private String titre;

    private JsonNode contenu;

    private Long idAuteur;

    private String nomAuteur;

    private LocalDateTime dateDiffusion;
}
