package mg.gov.mef.dsi.gpdi.security;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import org.springframework.stereotype.Component;

/**
 * Controle d'appartenance a un projet, utilise par tous les services dont les ressources sont
 * rattachees a un projet (jalons, livrables, sprints, taches et leurs sous-ressources). Le role
 * Keycloak (verifie par SecurityConfig) dit ce qu'un utilisateur peut faire en general ; ce service dit
 * s'il peut le faire sur CE projet precis.
 * <ul>
 *   <li>Ecriture : responsable du projet, chef de l'equipe rattachee, ou membre de cette equipe.</li>
 *   <li>Lecture : ecriture ci-dessus, plus le Directeur DSI (vue portefeuille).</li>
 *   <li>Lecture partageable (jalons/livrables) : lecture ci-dessus, plus un compte client avec un
 *       partage ACTIF sur ce projet precis (espace client).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AccesProjetService {

    private static final String ROLE_DIRECTEUR_DSI = "directeur_dsi";

    private final UtilisateurCourantService utilisateurCourantService;
    private final MembreEquipeRepository membreEquipeRepository;
    private final PartageClientRepository partageClientRepository;

    public Utilisateur utilisateurCourant() {
        return utilisateurCourantService.obtenirUtilisateurCourant();
    }

    public void verifierMembreDuProjet(Projet projet) {
        if (!estMembreDuProjet(projet, utilisateurCourant())) {
            throw new AccesProjetRefuseException(projet.getId());
        }
    }

    public void verifierAccesLecture(Projet projet) {
        Utilisateur utilisateur = utilisateurCourant();
        if (!estMembreDuProjet(projet, utilisateur) && !estDirecteurDsi(utilisateur)) {
            throw new AccesProjetRefuseException(projet.getId());
        }
    }

    public void verifierAccesLecturePartageable(Projet projet) {
        Utilisateur utilisateur = utilisateurCourant();
        if (estMembreDuProjet(projet, utilisateur) || estDirecteurDsi(utilisateur) || aUnPartageActif(projet, utilisateur)) {
            return;
        }
        throw new AccesProjetRefuseException(projet.getId());
    }

    private boolean estMembreDuProjet(Projet projet, Utilisateur utilisateur) {
        if (projet.getResponsable() != null && projet.getResponsable().getId().equals(utilisateur.getId())) {
            return true;
        }
        if (projet.getEquipe() == null) {
            return false;
        }
        if (projet.getEquipe().getChefEquipe() != null && projet.getEquipe().getChefEquipe().getId().equals(utilisateur.getId())) {
            return true;
        }
        return membreEquipeRepository.existsByEquipeIdAndUtilisateurId(projet.getEquipe().getId(), utilisateur.getId());
    }

    private boolean estDirecteurDsi(Utilisateur utilisateur) {
        return utilisateur.getRole() != null && ROLE_DIRECTEUR_DSI.equals(utilisateur.getRole().getNom());
    }

    private boolean aUnPartageActif(Projet projet, Utilisateur utilisateur) {
        return partageClientRepository.existsByClientIdAndProjetIdAndStatut(utilisateur.getId(), projet.getId(), StatutPartage.ACTIF);
    }
}
