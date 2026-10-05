package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.RecommandationProjetResponse;

import java.util.List;

/** Recommandations proactives de pilotage de projet (réaffectation, désengorgement de sprint, réestimation). */
public interface RecommandationProjetService {

    /** Recommandations en attente (PROPOSEE) du projet, de la plus récente à la plus ancienne. */
    List<RecommandationProjetResponse> lister(Long idProjet);

    /** Analyse le projet et propose de nouvelles recommandations (peut durer plusieurs minutes). */
    List<RecommandationProjetResponse> analyser(Long idProjet, Long idDemandeur);

    /** Applique une recommandation en attente : exécute l'action proposée sur le projet. */
    RecommandationProjetResponse appliquer(Long idProjet, Long idRecommandation, Long idValidateur);

    /** Rejette une recommandation en attente sans l'exécuter. */
    RecommandationProjetResponse rejeter(Long idProjet, Long idRecommandation, Long idValidateur);
}
