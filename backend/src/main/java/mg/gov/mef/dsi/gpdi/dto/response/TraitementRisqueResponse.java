package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TraitementRisqueResponse {

    private Long idRisque;

    /** Nouveau statut du risque : SOUS_CONTROLE apres application, REJETE apres rejet. */
    private String statut;

    /** Solutions appliquees, dans l'ordre. Vide pour un rejet. */
    private List<SolutionTraiteeResponse> solutions;
}
