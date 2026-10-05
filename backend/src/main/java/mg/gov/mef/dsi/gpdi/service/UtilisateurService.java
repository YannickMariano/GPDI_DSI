package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurCreationRequest;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurModificationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UtilisateurService {

    UtilisateurResponse creer(UtilisateurCreationRequest request);

    Page<UtilisateurResponse> rechercher(Pageable pageable);

    UtilisateurResponse obtenir(Long id);

    UtilisateurResponse modifier(Long id, UtilisateurModificationRequest request);

    /**
     * @param detacherTaches si {@code true}, retire l'utilisateur des taches auxquelles il est encore
     *                       assigne avant de le supprimer, plutot que de refuser la suppression.
     */
    void supprimer(Long id, boolean detacherTaches);

    UtilisateurResponse obtenirCourant();

    UtilisateurResponse activer(Long id);

    UtilisateurResponse suspendre(Long id);
}
