package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.JalonRequest;
import mg.gov.mef.dsi.gpdi.dto.response.JalonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JalonService {

    JalonResponse creer(Long idProjet, JalonRequest request);

    Page<JalonResponse> rechercher(Long idProjet, Pageable pageable);

    JalonResponse obtenir(Long idProjet, Long id);

    JalonResponse modifier(Long idProjet, Long id, JalonRequest request);

    void supprimer(Long idProjet, Long id);

    JalonResponse terminer(Long idProjet, Long id);

    JalonResponse annuler(Long idProjet, Long id);

    /**
     * Fait passer en EN_RETARD les jalons A_VENIR dont la date d'echeance est depassee.
     * Appele par la tache planifiee.
     */
    void marquerEnRetardLesJalonsEnRetard();
}
