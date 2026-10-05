package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.PointInformationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PointInformationResponse;
import mg.gov.mef.dsi.gpdi.entity.PointInformation;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesPointsInformationRefuseException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.PointInformationMapper;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import mg.gov.mef.dsi.gpdi.repository.PointInformationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
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
 * la regle metier - chef de projet du projet, ou compte client avec un partage actif sur ce projet precis -
 * est verifiee ici, comme pour les notifications. Cela evite d'accorder un role large a tous les comptes
 * clients pour une simple lecture, et cela verifie reellement le perimetre partage (contrairement aux
 * jalons/livrables, ouverts a tout titulaire du role, quel que soit le projet demande).
 */
@Service
@RequiredArgsConstructor
public class PointInformationServiceImpl implements PointInformationService {

    private static final Logger log = LoggerFactory.getLogger(PointInformationServiceImpl.class);

    private final PointInformationRepository pointInformationRepository;
    private final ProjetRepository projetRepository;
    private final PartageClientRepository partageClientRepository;
    private final PointInformationMapper pointInformationMapper;
    private final UtilisateurCourantService utilisateurCourantService;

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
        Utilisateur utilisateurCourant = utilisateurCourantService.obtenirUtilisateurCourant();
        if (!estResponsableDuProjet(projet, utilisateurCourant) && !aUnPartageActif(projet, utilisateurCourant)) {
            throw new AccesPointsInformationRefuseException(idProjet);
        }
        return pointInformationRepository.findByProjetIdOrderByDateCreationDesc(idProjet, pageable)
                .map(pointInformationMapper::toResponse);
    }

    private boolean estResponsableDuProjet(Projet projet, Utilisateur utilisateur) {
        return projet.getResponsable() != null && projet.getResponsable().getId().equals(utilisateur.getId());
    }

    private boolean aUnPartageActif(Projet projet, Utilisateur utilisateur) {
        return partageClientRepository.existsByClientIdAndProjetIdAndStatut(utilisateur.getId(), projet.getId(), StatutPartage.ACTIF);
    }

    private Projet obtenirProjet(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
    }
}
