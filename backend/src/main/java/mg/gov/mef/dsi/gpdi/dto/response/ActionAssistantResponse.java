package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.StatutActionAssistant;
import mg.gov.mef.dsi.gpdi.entity.TypeActionAssistant;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ActionAssistantResponse {

    private Long id;

    private TypeActionAssistant type;

    /** Resume de l'action, ex. « Affecter A. Koto à T-42 — Refonte écran login ». */
    private String libelle;

    /** Details lisibles de ce qui sera fait (projet, valeurs appliquees...). */
    private List<String> details;

    /** Action definitive (suppression, cloture ou archivage de projet). */
    private boolean irreversible;

    private StatutActionAssistant statut;

    /** Resultat de l'execution (ex. « Tâche T-57 créée. »), une fois l'action confirmee. */
    private String resultat;

    /** Projet concerne par l'action (pour que le client recharge ses donnees apres execution). */
    private Long idProjet;

    private LocalDateTime dateTraitement;
}
