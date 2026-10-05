package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class JalonRequest {

    @NotBlank
    @Size(max = 200)
    private String titre;

    @Size(max = 1000)
    private String description;

    @NotNull
    private LocalDate dateEcheance;
}
