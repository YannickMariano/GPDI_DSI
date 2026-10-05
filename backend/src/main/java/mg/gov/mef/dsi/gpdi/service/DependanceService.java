package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.DependanceRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TachePrecedenceResponse;

import java.util.List;

public interface DependanceService {

    TachePrecedenceResponse creer(Long idTache, DependanceRequest request);

    List<TachePrecedenceResponse> rechercher(Long idTache);

    void supprimer(Long idTache, Long idAutreTache);
}
