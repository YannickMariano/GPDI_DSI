package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.PartageClientRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PartageClientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartageClientService {

    List<PartageClientResponse> creer(PartageClientRequest request);

    Page<PartageClientResponse> rechercher(Pageable pageable);

    PartageClientResponse revoquer(Long id);

    /**
     * Fait passer en EXPIRE les partages ACTIF dont la date d'echeance est depassee.
     * Appele par la tache planifiee.
     */
    void expirerLesPartagesEnRetard();
}
