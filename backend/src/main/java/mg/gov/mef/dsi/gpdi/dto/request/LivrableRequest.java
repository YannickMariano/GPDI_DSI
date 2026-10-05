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
public class LivrableRequest {

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 100)
    private String type;

    @NotNull
    private LocalDate dateLivraison;
}
