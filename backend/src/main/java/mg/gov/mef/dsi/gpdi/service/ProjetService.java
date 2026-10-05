package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.ProjetRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ProjetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjetService {

    ProjetResponse creer(ProjetRequest request);

    Page<ProjetResponse> rechercher(Pageable pageable);

    ProjetResponse obtenir(Long id);

    ProjetResponse modifier(Long id, ProjetRequest request);

    ProjetResponse suspendre(Long id);

    ProjetResponse cloturer(Long id);

    ProjetResponse archiver(Long id);

    /**
     * Fait passer en EN_COURS les projets EN_PREPARATION dont la date de debut est arrivee,
     * et active leur equipe. Appele par la tache planifiee.
     */
    void demarrerLesProjetsArrivesADateDeDebut();
}
