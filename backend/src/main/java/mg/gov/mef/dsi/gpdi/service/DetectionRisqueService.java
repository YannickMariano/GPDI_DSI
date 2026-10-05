package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.ResultatDetectionRisqueResponse;

public interface DetectionRisqueService {

    /**
     * Analyse un projet avec l'assistant IA : cree les nouveaux risques detectes et reevalue ceux qui
     * sont sous controle. Un seul appel a la fois par projet. L'appel peut durer plusieurs minutes.
     *
     * @param idDemandeur utilisateur a l'origine de la demande, ou null pour une analyse automatique
     * @throws mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException si le projet n'existe pas
     * @throws mg.gov.mef.dsi.gpdi.exception.AnalyseDejaEnCoursException si le projet est deja en cours d'analyse
     * @throws mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException si l'assistant IA ne repond pas
     * @throws mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException si sa reponse est inexploitable
     */
    ResultatDetectionRisqueResponse detecter(Long idProjet, Long idDemandeur);
}
