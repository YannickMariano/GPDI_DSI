package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class JournalConnexionResponse {

    private LocalDateTime horodatage;

    private String matricule;

    private String nomAffiche;

    private String evenement;

    private String ip;

    private String resultat;
}
