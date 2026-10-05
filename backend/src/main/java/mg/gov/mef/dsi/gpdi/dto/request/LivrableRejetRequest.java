package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class LivrableRejetRequest {

    @NotBlank
    @Size(max = 500)
    private String motifRejet;
}
