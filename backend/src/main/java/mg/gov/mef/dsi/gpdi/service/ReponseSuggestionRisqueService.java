package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque;

import java.util.Set;

public interface ReponseSuggestionRisqueService {

    /**
     * Verifie et interprete la reponse JSON brute du modele. Une solution dont la reaffectation vise une
     * tache ou un membre inconnu devient une tache de suivi ; les solutions sans libelle ou en double
     * sont ecartees.
     *
     * @param idsMembres          membres de l'equipe pouvant etre designes
     * @param idsTachesReaffectables taches ouvertes que le modele a pu voir et que l'on peut reaffecter
     * @throws mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException si la reponse n'est pas un JSON du format
     *                                                                  attendu ou ne contient aucune solution exploitable
     */
    ReponseSuggestionRisque interpreter(String reponseJson, Set<Long> idsMembres, Set<Long> idsTachesReaffectables);
}
