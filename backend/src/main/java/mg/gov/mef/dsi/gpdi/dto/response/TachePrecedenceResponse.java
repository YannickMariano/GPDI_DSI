package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mg.gov.mef.dsi.gpdi.entity.TypeLien;

@Getter
@AllArgsConstructor
public class TachePrecedenceResponse {

    private Long idTacheSource;

    private String codeTacheSource;

    private String titreTacheSource;

    private Long idTacheCible;

    private String codeTacheCible;

    private String titreTacheCible;

    private TypeLien typeLien;
}
