package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;

import java.util.List;

public interface SuggestionRisqueService {

    /**
     * Renvoie les solutions proposees pour un risque ouvert. Si des solutions sont deja en attente pour ce
     * risque, elles sont renvoyees telles quelles sans interroger l'assistant IA ; sinon il est interroge
     * (plusieurs secondes a plusieurs minutes) et sa reponse est conservee.
     *
     * @param idDemandeur utilisateur a l'origine de la demande
     * @throws mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException si le risque n'existe pas ou n'appartient pas au projet
     * @throws mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException si le risque n'est pas OUVERT
     * @throws mg.gov.mef.dsi.gpdi.exception.SuggestionDejaEnCoursException si des solutions sont deja en cours de recherche
     * @throws mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException si l'assistant IA ne repond pas
     * @throws mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException si sa reponse est inexploitable
     */
    List<SolutionRisqueResponse> suggerer(Long idProjet, Long idRisque, Long idDemandeur);
}
