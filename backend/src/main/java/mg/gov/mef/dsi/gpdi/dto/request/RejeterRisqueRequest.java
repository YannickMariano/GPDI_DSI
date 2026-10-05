package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class RejeterRisqueRequest {

    /** Optionnel : raison du rejet, conservee avec les solutions rejetees. */
    @Size(max = 1000)
    private String motif;
}
