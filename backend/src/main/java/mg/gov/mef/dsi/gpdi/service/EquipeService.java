package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.EquipeRequest;
import mg.gov.mef.dsi.gpdi.dto.response.EquipeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EquipeService {

    EquipeResponse creer(EquipeRequest request);

    Page<EquipeResponse> rechercher(Pageable pageable);

    EquipeResponse obtenir(Long id);

    EquipeResponse modifier(Long id, EquipeRequest request);

    void supprimer(Long id);
}
