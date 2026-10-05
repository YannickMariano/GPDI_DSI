package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SousTacheResponse {

    private Long id;

    private String titre;

    private Boolean fait;
}
