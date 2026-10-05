package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.SprintRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SprintResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SprintService {

    SprintResponse creer(Long idProjet, SprintRequest request);

    Page<SprintResponse> rechercher(Long idProjet, Pageable pageable);

    SprintResponse obtenir(Long idProjet, Long id);

    SprintResponse modifier(Long idProjet, Long id, SprintRequest request);

    void supprimer(Long idProjet, Long id);

    SprintResponse commencer(Long idProjet, Long id);

    SprintResponse terminer(Long idProjet, Long id);
}
