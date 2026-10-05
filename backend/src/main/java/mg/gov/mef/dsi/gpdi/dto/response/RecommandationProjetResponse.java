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
public class RecommandationProjetResponse {

    private Long id;

    /** REASSIGNER_TACHE, RETIRER_DU_SPRINT ou AJUSTER_ESTIMATION. */
    private String type;

    private String libelle;

    private String justification;

    private String preuve;

    /** PROPOSEE, APPLIQUEE ou REJETEE. */
    private String statut;

    /** Ce qui a ete fait, renseigne uniquement juste apres l'application. */
    private String resultat;

    private LocalDateTime dateCreation;
}
