package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProjetResponse {

    private Long id;

    private String titre;

    private String codeReference;

    private String description;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private LocalDate dateCloture;

    private StatutProjet statut;

    private Priorite priorite;

    private Integer progression;

    private Boolean archive;

    private LocalDateTime dateCreation;

    private Long idResponsable;

    private String nomResponsable;

    private Long idEquipe;

    private String nomEquipe;

    /** Derivee du responsable (Utilisateur.specialite), jamais saisie separement. */
    private String direction;

    /**
     * Indicateur de sante derive (sain / sous_surveillance / a_risque), calcule a la volee a
     * partir de l'echeance — pas de colonne dediee. A affiner quand Sprints/Jalons/Taches
     * seront relies aux projets (retards de taches, risques ouverts...).
     */
    private String sante;
}
