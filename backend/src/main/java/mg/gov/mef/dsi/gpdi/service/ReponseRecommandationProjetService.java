package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet;

import java.util.List;
import java.util.Set;

public interface ReponseRecommandationProjetService {

    /**
     * Vérifie et interprète la réponse JSON brute du modèle. Une recommandation dont la tâche (ou, pour
     * une réaffectation, le membre) n'est pas réelle est écartée, de même qu'un doublon d'une
     * recommandation déjà proposée ou déjà retenue dans cette même réponse.
     *
     * @param idsTaches   tâches ouvertes que le modèle a pu voir
     * @param idsMembres  membres de l'équipe pouvant être désignés
     * @param dejaProposees libellés des recommandations déjà en attente pour ce projet
     * @throws mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException si la réponse n'est pas un JSON du
     *                                                                  format attendu ou ne contient aucune recommandation exploitable
     */
    ReponseRecommandationProjet interpreter(String reponseJson, Set<Long> idsTaches, Set<Long> idsMembres, List<String> dejaProposees);
}
