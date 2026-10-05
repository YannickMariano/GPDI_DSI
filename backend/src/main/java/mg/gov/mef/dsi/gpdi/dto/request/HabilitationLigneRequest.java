package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Une case modifiee de la matrice d'habilitations (mise a jour groupee via HabilitationController). */
@Getter
@Setter
@NoArgsConstructor
public class HabilitationLigneRequest {

    @NotBlank
    private String fonction;

    @NotBlank
    private String roleNom;

    @NotNull
    private String niveau;
}
