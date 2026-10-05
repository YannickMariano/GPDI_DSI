package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.TypeLien;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
public class DependanceRequest {

    @NotNull
    private Long idTacheCible;

    @NotNull
    private TypeLien typeLien;
}
