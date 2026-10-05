package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.MessageAssistantResponse;

import java.util.List;

public interface AssistantIaService {

    /** Historique des échanges du projet, du plus ancien au plus récent (réservé au chef de projet du projet). */
    List<MessageAssistantResponse> historique(Long idProjet);

    /**
     * Pose une question libre à l'assistant IA sur le projet (peut durer jusqu'à plusieurs minutes) et
     * conserve l'échange. Réservé au chef de projet du projet.
     */
    MessageAssistantResponse poserQuestion(Long idProjet, String question, Long idDemandeur);
}
