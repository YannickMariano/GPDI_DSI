package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultatDetectionRisqueResponse {

    private Long idAnalyse;

    private int nombreNouveauxRisques;

    /** Risques sous controle dont la probabilite et l'impact ont ete recalcules (y compris ceux clos ci-dessous). */
    private int nombreRisquesReevalues;

    /** Risques sous controle passes a CLOS car leur niveau est redevenu FAIBLE. */
    private int nombreRisquesClos;

    private String synthese;
}
