package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LivrableResponse {

    private Long id;

    private String description;

    private String type;

    private StatutLivrable statut;

    private LocalDate dateLivraison;

    private LocalDate dateValidation;

    private LocalDate dateRejet;

    private String motifRejet;

    private Long idJalon;
}
