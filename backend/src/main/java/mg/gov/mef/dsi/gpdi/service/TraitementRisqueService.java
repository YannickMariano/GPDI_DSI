package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.TraitementRisqueResponse;

import java.util.List;

public interface TraitementRisqueService {

    /**
     * Execute sur le projet les solutions cochees (creation ou reaffectation de taches), tout ou rien :
     * si l'une echoue, aucune n'est appliquee. Le risque passe alors a SOUS_CONTROLE et les solutions non
     * cochees sont ecartees. Une reaffectation impossible (tache cloturee, membre hors equipe) devient une
     * tache de suivi.
     *
     * @throws mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException si le risque n'existe pas ou n'appartient pas au projet
     * @throws mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException si le risque n'est pas OUVERT
     * @throws mg.gov.mef.dsi.gpdi.exception.SolutionRisqueIntrouvableException si une solution n'est pas celle de ce risque
     * @throws mg.gov.mef.dsi.gpdi.exception.StatutRecommandationInvalideException si une solution n'est plus en attente
     */
    TraitementRisqueResponse appliquer(Long idProjet, Long idRisque, List<Long> idsSolutions, Long idValidateur);

    /**
     * Rejette un risque ouvert : il passe a REJETE (l'IA ne le recreera plus) et ses solutions en attente
     * sont rejetees.
     *
     * @param motif raison du rejet, ou null
     * @throws mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException si le risque n'existe pas ou n'appartient pas au projet
     * @throws mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException si le risque n'est pas OUVERT
     */
    TraitementRisqueResponse rejeter(Long idProjet, Long idRisque, String motif, Long idValidateur);
}
