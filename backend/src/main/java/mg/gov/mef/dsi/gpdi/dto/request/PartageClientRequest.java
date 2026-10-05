package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PartageClientRequest {

    @NotNull
    private Long idUtilisateur;

    @NotEmpty
    private List<Long> idsProjets;

    @NotNull
    private LocalDate dateEcheance;
}
