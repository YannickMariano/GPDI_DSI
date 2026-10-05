package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolutionTraiteeResponse {

    private Long id;

    private String libelle;

    /** Ce qui a reellement ete fait : CREER_TACHE ou REASSIGNER_TACHE. */
    private String typeAction;

    /** Tache creee ou reaffectee. */
    private Long idTache;

    private String codeTache;

    /** Vrai si la solution n'etait pas executable telle quelle et a ete remplacee par une tache de suivi. */
    private boolean suivi;

    /** Explication lorsque l'execution differe de la solution proposee. Absent sinon. */
    private String remarque;
}
