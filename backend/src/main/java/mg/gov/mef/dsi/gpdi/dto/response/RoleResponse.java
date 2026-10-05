package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoleResponse {

    private Long id;

    private String nom;

    private String description;
}