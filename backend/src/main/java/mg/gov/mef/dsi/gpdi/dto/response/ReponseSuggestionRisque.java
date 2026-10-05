package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandation;

import java.util.List;

/**
 * Reponse du modele apres verification : au moins une solution, chacune executable (tache et membre
 * reels) ou ramenee a la creation d'une tache de suivi. Ce n'est pas un objet expose par l'API.
 */
@Getter
@AllArgsConstructor
public class ReponseSuggestionRisque {

    private final List<SolutionProposee> solutions;

    @Getter
    @AllArgsConstructor
    public static class SolutionProposee {

        private final String libelle;

        private final String justification;

        private final String gain;

        private final TypeActionRecommandation type;

        /** REASSIGNER_TACHE : tache existante a reaffecter. Null pour CREER_TACHE. */
        private final Long idTache;

        /** Membre de l'equipe concerne : obligatoire pour REASSIGNER_TACHE, optionnel pour CREER_TACHE. */
        private final Long idAssigne;

        /** CREER_TACHE : titre de la tache a creer. Null pour REASSIGNER_TACHE. */
        private final String titreTache;

        private final String descriptionTache;

        /** En heures, strictement positif, ou null. */
        private final Double tempsEstime;
    }
}
