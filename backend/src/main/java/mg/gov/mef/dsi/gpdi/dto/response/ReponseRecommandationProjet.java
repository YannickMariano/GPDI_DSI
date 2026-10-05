package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandationProjet;

import java.util.List;

/**
 * Reponse du modele apres verification : chaque recommandation est executable (tache et, le cas
 * echeant, membre reels). Ce n'est pas un objet expose par l'API.
 */
@Getter
@AllArgsConstructor
public class ReponseRecommandationProjet {

    private final List<RecommandationProposee> recommandations;

    @Getter
    @AllArgsConstructor
    public static class RecommandationProposee {

        private final TypeActionRecommandationProjet type;

        private final String libelle;

        private final String justification;

        private final String preuve;

        /** Tache concernee (toujours requise, les trois types d'action portent sur une tache existante). */
        private final Long idTache;

        /** REASSIGNER_TACHE uniquement. */
        private final Long idAssigne;

        /** AJUSTER_ESTIMATION uniquement, en heures, strictement positif. */
        private final Double nouvelleEstimation;
    }
}
