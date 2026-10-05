package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TacheResponse {

    private Long id;

    private String code;

    private String titre;

    private String description;

    private StatutTache statut;

    private Priorite priorite;

    private LocalDate dateEcheance;

    private Double tempsEstime;

    private Double tempsPasse;

    private LocalDateTime demarreeLe;

    private Long idSprint;

    private String nomSprint;

    private Long idProjet;

    private List<UtilisateurAssigneResponse> assignations;
}
