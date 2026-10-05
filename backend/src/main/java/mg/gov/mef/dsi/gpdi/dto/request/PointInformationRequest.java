package mg.gov.mef.dsi.gpdi.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class PointInformationRequest {

    @NotBlank
    @Size(max = 300)
    private String titre;

    @NotBlank
    @Size(max = 4000)
    private String corps;
}
