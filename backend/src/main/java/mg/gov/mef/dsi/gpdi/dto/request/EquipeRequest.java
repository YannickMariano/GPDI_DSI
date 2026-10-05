package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class EquipeRequest {

    @NotBlank
    @Size(max = 150)
    private String nom;

    @Size(max = 500)
    private String description;

    @NotNull
    private Long idChefEquipe;

    @NotEmpty
    private List<Long> idsMembres;
}
