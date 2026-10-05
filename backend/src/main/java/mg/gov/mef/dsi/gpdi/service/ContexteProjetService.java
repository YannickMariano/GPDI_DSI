package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;

public interface ContexteProjetService {

    /**
     * Construit, en lecture seule, la photographie d'un projet destinee a l'assistant IA
     * (avancement, equipe et charge, taches a surveiller, sprints, jalons, risques deja connus).
     */
    ContexteProjetIa construire(Long idProjet);
}
