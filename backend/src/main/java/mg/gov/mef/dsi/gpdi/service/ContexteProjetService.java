package mg.gov.mef.dsi.gpdi.service;

import com.fasterxml.jackson.databind.JsonNode;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;

import java.util.List;

public interface ContexteProjetService {

    /**
     * Construit, en lecture seule, la photographie d'un projet destinee a l'assistant IA
     * (avancement, equipe et charge, taches a surveiller, sprints, jalons, risques deja connus).
     */
    ContexteProjetIa construire(Long idProjet);

    /**
     * Version condensee de {@link #construire(Long)} pour plusieurs projets a la fois (assistant IA global) :
     * meme contenu par projet, reduit a ce qui est actif ou inquietant pour tenir dans la fenetre du modele.
     */
    JsonNode construirePortefeuille(List<Long> idsProjets);
}
