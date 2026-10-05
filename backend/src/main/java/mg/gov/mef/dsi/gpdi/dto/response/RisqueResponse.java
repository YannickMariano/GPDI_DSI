package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RisqueResponse {

    private Long id;

    /** R1, R2... : rang du risque dans son projet, par ordre de detection. Calcule a la lecture, non stocke. */
    private String code;

    private String libelle;

    /** Mesure preventive (colonne mesure_mitigation). */
    private String parade;

    /** Explication du risque (colonne description). */
    private String analyse;

    private Integer probabilite;

    private Integer impact;

    private NiveauRisque niveau;

    private StatutRisque statut;

    private LocalDateTime dateDetection;

    private Long idPorteur;

    /** Prenom et nom du porteur, ou null s'il n'y en a pas. */
    private String porteur;

    /** Solutions en attente de decision pour ce risque (deja proposees par l'IA). Vide sinon. */
    private List<SolutionRisqueResponse> solutions;
}
