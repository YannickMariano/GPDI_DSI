package mg.gov.mef.dsi.gpdi.security;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurCourantIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UtilisateurCourantService {

    private final UtilisateurRepository utilisateurRepository;

    public Utilisateur obtenirUtilisateurCourant() {
        JwtAuthenticationToken authentification = (JwtAuthenticationToken) SecurityContextHolder.getContext()
                .getAuthentication();
        String matricule = authentification.getToken().getClaimAsString("preferred_username");
        return utilisateurRepository.findByMatricule(matricule)
                .orElseThrow(() -> new UtilisateurCourantIntrouvableException(matricule));
    }
}
