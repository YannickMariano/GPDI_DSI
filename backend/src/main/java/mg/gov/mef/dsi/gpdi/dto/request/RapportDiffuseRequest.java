package mg.gov.mef.dsi.gpdi.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class RapportDiffuseRequest {

    @NotNull
    private Long idProjet;

    @NotBlank
    @Size(max = 50)
    private String modele;

    @NotBlank
    @Size(max = 200)
    private String nom;

    @Size(max = 300)
    private String titre;

    /** Document du rapport tel qu'affiché par le frontend (chiffres, sections, tableaux), enregistré tel quel. */
    @NotNull
    private JsonNode contenu;
}
