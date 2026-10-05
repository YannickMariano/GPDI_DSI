package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointInformationResponse {

    private Long id;

    private String titre;

    private String corps;

    private LocalDateTime dateCreation;

    private Long idAuteur;

    private String nomAuteur;
}
