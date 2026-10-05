package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.HabilitationLigneRequest;
import mg.gov.mef.dsi.gpdi.dto.response.HabilitationResponse;

import java.util.List;

public interface HabilitationService {

    List<HabilitationResponse> rechercherMatrice();

    void mettreAJourMatrice(List<HabilitationLigneRequest> lignes);

    /**
     * Verifie que l'utilisateur courant (role metier resolu via {@code Utilisateur.role.nom}) a le droit
     * d'ecriture sur la fonction donnee, sinon leve {@link mg.gov.mef.dsi.gpdi.exception.HabilitationRefuseeException}.
     */
    void verifierEcriture(String fonction);
}
