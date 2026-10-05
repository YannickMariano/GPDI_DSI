package mg.gov.mef.dsi.gpdi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reponse du modele apres verification : ne contient que des elements valides (notes de 1 a 5,
 * identifiants reels, pas de doublon). Ce n'est pas un objet expose par l'API.
 */
@Getter
@AllArgsConstructor
public class ReponseDetectionRisque {

    private final List<RisqueDetecte> nouveauxRisques;

    private final List<ReevaluationRisque> reevaluations;

    /** Texte de synthese, conserve dans ia_analyses.reponse. */
    private final String synthese;

    /** Entre 0 et 1, ou null si le modele n'en a pas donne ou a donne une valeur hors bornes. */
    private final BigDecimal indiceConfiance;

    @Getter
    @AllArgsConstructor
    public static class RisqueDetecte {

        private final String libelle;

        private final String description;

        private final int probabilite;

        private final int impact;

        private final String parade;

        /** Membre reel de l'equipe du projet, ou null (l'appelant applique alors son porteur par defaut). */
        private final Long idPorteur;
    }

    @Getter
    @AllArgsConstructor
    public static class ReevaluationRisque {

        private final Long idRisque;

        private final int probabilite;

        private final int impact;
    }
}
