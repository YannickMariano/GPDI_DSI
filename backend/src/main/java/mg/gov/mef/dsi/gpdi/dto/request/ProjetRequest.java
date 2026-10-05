package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ProjetRequest {

    @NotBlank
    @Size(max = 200)
    private String titre;

    @Size(max = 1000)
    private String description;

    @NotNull
    private LocalDate dateDebut;

    @NotNull
    private LocalDate dateFin;

    @NotNull
    private Priorite priorite;

    /**
     * Optionnel : EN_PREPARATION par defaut si absent.
     * CLOTURE et ARCHIVE sont interdits ici (actions dediees /cloturer et /archiver).
     */
    private StatutProjet statut;

    @NotNull
    private Long idResponsable;

    /** Optionnel : un projet peut etre cree sans equipe assignee. */
    private Long idEquipe;
}
