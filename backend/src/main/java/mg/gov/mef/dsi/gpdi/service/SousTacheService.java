package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.SousTacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SousTacheResponse;

import java.util.List;

public interface SousTacheService {

    SousTacheResponse creer(Long idTache, SousTacheRequest request);

    List<SousTacheResponse> rechercher(Long idTache);

    void supprimer(Long idTache, Long id);

    SousTacheResponse basculer(Long idTache, Long id);
}
