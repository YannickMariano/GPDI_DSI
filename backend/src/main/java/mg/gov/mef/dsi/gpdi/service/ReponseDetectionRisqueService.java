package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque;

public interface ReponseDetectionRisqueService {

    /**
     * Verifie et interprete la reponse JSON brute du modele. Les elements invalides (notes hors de 1 a 5,
     * libelle vide, doublon d'un risque connu, identifiant inconnu...) sont ecartes individuellement ;
     * seule une reponse inexploitable dans son ensemble leve une exception.
     *
     * @throws mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException si la reponse n'est pas un JSON du format attendu
     */
    ReponseDetectionRisque interpreter(String reponseJson, ContexteProjetIa contexte);
}
