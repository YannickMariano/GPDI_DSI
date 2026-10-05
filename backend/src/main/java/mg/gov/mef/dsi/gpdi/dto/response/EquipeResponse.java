package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class EquipeResponse {

    private Long id;

    private String nom;

    private String description;

    private Boolean active;

    private LocalDateTime dateCreation;

    private Long idChefEquipe;

    private String nomChefEquipe;

    private List<MembreEquipeResponse> membres;
}
