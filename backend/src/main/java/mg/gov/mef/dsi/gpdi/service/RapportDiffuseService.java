package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.RapportDiffuseRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RapportDiffuseResponse;

import java.util.List;

public interface RapportDiffuseService {

    /** Diffuse un rapport (photo figee) sur un projet : reserve au chef de projet responsable de ce projet. */
    RapportDiffuseResponse diffuser(RapportDiffuseRequest request);

    /**
     * Rapports diffuses visibles par l'utilisateur courant, du plus recent au plus ancien : tous pour le
     * Directeur DSI ; sinon ceux des projets dont il est responsable, chef ou membre de l'equipe, ou sur
     * lesquels une tache lui est assignee.
     */
    List<RapportDiffuseResponse> rechercherPourUtilisateurCourant();
}
