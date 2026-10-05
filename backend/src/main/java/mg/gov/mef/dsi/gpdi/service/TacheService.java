package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TacheDetailResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TacheService {

    TacheResponse creer(Long idProjet, TacheRequest request);

    Page<TacheResponse> rechercher(Long idProjet, Pageable pageable);

    TacheDetailResponse obtenir(Long idProjet, Long id);

    TacheResponse modifier(Long idProjet, Long id, TacheRequest request);

    void supprimer(Long idProjet, Long id);
}
