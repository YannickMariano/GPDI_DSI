package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AppliquerSolutionsRequest {

    /** Identifiants des solutions cochees par l'utilisateur (champ "id" de la liste des solutions proposees). */
    @NotEmpty
    private List<@NotNull Long> idsSolutions;
}
