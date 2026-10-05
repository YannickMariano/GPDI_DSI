package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;

import java.util.List;

public interface RisqueService {

    /**
     * Tous les risques d'un projet, par ordre de detection (R1, R2...), sans pagination : un projet en
     * compte peu. Chaque risque porte ses solutions en attente de decision.
     *
     * @throws mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException si le projet n'existe pas
     */
    List<RisqueResponse> rechercher(Long idProjet);
}
