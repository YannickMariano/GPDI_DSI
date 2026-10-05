package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.LivrableRejetRequest;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRequest;
import mg.gov.mef.dsi.gpdi.dto.response.LivrableResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LivrableService {

    LivrableResponse creer(Long idJalon, LivrableRequest request);

    Page<LivrableResponse> rechercher(Long idJalon, Pageable pageable);

    LivrableResponse obtenir(Long idJalon, Long id);

    LivrableResponse modifier(Long idJalon, Long id, LivrableRequest request);

    void supprimer(Long idJalon, Long id);

    LivrableResponse livrer(Long idJalon, Long id);

    LivrableResponse rejeter(Long idJalon, Long id, LivrableRejetRequest request);
}
