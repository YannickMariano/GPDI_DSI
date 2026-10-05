package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SprintResponse {

    private Long id;

    private String nom;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private String objectif;

    private Integer capaciteHeures;

    private Integer progression;

    private StatutSprint statut;

    private LocalDateTime dateCreation;

    private Long idProjet;
}
