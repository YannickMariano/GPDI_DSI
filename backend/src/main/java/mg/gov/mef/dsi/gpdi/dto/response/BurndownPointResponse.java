package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BurndownPointResponse {

    private LocalDate date;

    /**
     * Travail restant reellement observe ce jour-la (heures). Null si le jour est futur ou
     * anterieur a la mise en place de l'instantane quotidien (pas d'historique reconstituable).
     */
    private Double restant;

    /** Trajectoire ideale (lineaire) du travail restant, toujours calculable. */
    private double ideal;
}
