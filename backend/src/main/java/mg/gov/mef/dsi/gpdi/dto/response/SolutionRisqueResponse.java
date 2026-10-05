package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolutionRisqueResponse {

    /** Identifiant de la recommandation : c'est lui que le frontend renverra pour appliquer ou rejeter la solution. */
    private Long id;

    private String libelle;

    private String justification;

    /** Benefice attendu (gain de temps), en texte libre. Peut etre absent. */
    private String gain;

    /** CREER_TACHE ou REASSIGNER_TACHE. */
    private String typeAction;

    /** PROPOSEE, APPLIQUEE ou REJETEE : permet d'afficher l'historique complet, y compris les solutions deja traitees. */
    private String statut;

    /** Code de la tache creee ou reaffectee, renseigne uniquement si la solution a ete appliquee. */
    private String codeTache;

    /** Motif de rejet, renseigne uniquement si la solution (ou le risque) a ete rejete. */
    private String motifRejet;
}
