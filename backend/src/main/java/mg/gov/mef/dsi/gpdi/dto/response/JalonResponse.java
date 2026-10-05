package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class JalonResponse {

    private Long id;

    private String titre;

    private String description;

    private LocalDate dateEcheance;

    private StatutJalon statut;

    private LocalDate dateValidation;

    private Long idProjet;
}
