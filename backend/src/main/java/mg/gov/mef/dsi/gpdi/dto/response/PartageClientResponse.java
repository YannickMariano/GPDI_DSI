package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PartageClientResponse {

    private Long id;

    private Long idUtilisateur;

    private String nomClient;

    private Long idProjet;

    private String titreProjet;

    private StatutPartage statut;

    private LocalDateTime dateCreation;

    private LocalDate dateEcheance;
}
