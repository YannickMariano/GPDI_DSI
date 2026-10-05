package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.PointInformationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PointInformationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointInformationService {

    /** Réservé au chef de projet (responsable) du projet. */
    PointInformationResponse creer(Long idProjet, PointInformationRequest request);

    /** Réservé au chef de projet du projet, ou à un compte client avec un partage actif sur ce projet. */
    Page<PointInformationResponse> rechercher(Long idProjet, Pageable pageable);
}
