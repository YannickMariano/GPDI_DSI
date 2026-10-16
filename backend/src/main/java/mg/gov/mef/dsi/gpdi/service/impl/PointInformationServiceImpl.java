package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.PointInformationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PointInformationResponse;
import mg.gov.mef.dsi.gpdi.entity.PointInformation;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesPointsInformationRefuseException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.PointInformationMapper;
import mg.gov.mef.dsi.gpdi.repository.PointInformationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.PointInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * L'acces n'est pas restreint par un role Keycloak dedie (l'endpoint ne demande que d'etre authentifie) :
 * la regle metier est verifiee ici, comme pour les notifications.
 * Publication : reservee au chef de projet (le seul a diffuser un point d'information au client).
 * Lecture : ouverte a tous les membres du projet (vue d'ensemble du projet), au Directeur DSI, et a un
 * compte client avec un partage actif sur ce projet precis (meme perimetre que jalons/livrables/sprints).
 */
@Service
@RequiredArgsConstructor
public class PointInformationServiceImpl implements PointInformationService {

    private static final Logger log = LoggerFactory.getLogger(PointInformationServiceImpl.class);

    private final PointInformationRepository pointInformationRepository;
    private final ProjetRepository projetRepository;
    private final PointInformationMapper pointInformationMapper;
    private final UtilisateurCourantService utilisateurCourantService;
    private final AccesProjetService accesProjetService;

    @Override
    @Transactional
    public PointInformationResponse creer(Long idProjet, PointInformationRequest request) {
        Projet projet = obtenirProjet(idProjet);
        Utilisateur auteur = utilisateurCourantService.obtenirUtilisateurCourant();
        if (!estResponsableDuProjet(projet, auteur)) {
            throw new AccesPointsInformationRefuseException(idProjet);
        }

        PointInformation point = new PointInformation();
        point.setProjet(projet);
        point.setAuteur(auteur);
        point.setTitre(request.getTitre());
        point.setCorps(request.getCorps());

        PointInformationResponse reponse = pointInformationMapper.toResponse(pointInformationRepository.save(point));
        log.info("Point d'information {} publie sur le projet {} par l'utilisateur {}", reponse.getId(), idProjet, auteur.getId());
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PointInformationResponse> rechercher(Long idProjet, Pageable pageable) {
        Projet projet = obtenirProjet(idProjet);
        accesProjetService.verifierAccesLecturePartageable(projet);
        return pointInformationRepository.findByProjetIdOrderByDateCreationDesc(idProjet, pageable)
                .map(pointInformationMapper::toResponse);
    }

    private boolean estResponsableDuProjet(Projet projet, Utilisateur utilisateur) {
        return projet.getResponsable() != null && projet.getResponsable().getId().equals(utilisateur.getId());
    }

    private Projet obtenirProjet(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
    }
}
