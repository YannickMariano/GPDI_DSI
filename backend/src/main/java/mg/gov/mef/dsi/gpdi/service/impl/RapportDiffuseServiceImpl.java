package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.RapportDiffuseRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RapportDiffuseResponse;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.RapportDiffuse;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.RapportDiffuseMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RapportDiffuseRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import mg.gov.mef.dsi.gpdi.service.RapportDiffuseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comme les points d'information, l'endpoint ne demande que d'etre authentifie : la regle metier
 * (chef de projet responsable pour diffuser ; membres concernes du projet ou Directeur DSI pour lire)
 * est verifiee ici.
 */
@Service
@RequiredArgsConstructor
public class RapportDiffuseServiceImpl implements RapportDiffuseService {

    private static final Logger log = LoggerFactory.getLogger(RapportDiffuseServiceImpl.class);

    private static final String ROLE_DIRECTEUR_DSI = "directeur_dsi";

    private final RapportDiffuseRepository rapportDiffuseRepository;
    private final ProjetRepository projetRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final AffectationTacheRepository affectationTacheRepository;
    private final RapportDiffuseMapper rapportDiffuseMapper;
    private final UtilisateurCourantService utilisateurCourantService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RapportDiffuseResponse diffuser(RapportDiffuseRequest request) {
        Projet projet = projetRepository.findById(request.getIdProjet())
                .orElseThrow(() -> new ProjetIntrouvableException(request.getIdProjet()));
        Utilisateur auteur = utilisateurCourantService.obtenirUtilisateurCourant();
        if (!estResponsable(projet, auteur)) {
            throw new AccesProjetRefuseException(projet.getId());
        }

        RapportDiffuse rapport = new RapportDiffuse();
        rapport.setProjet(projet);
        rapport.setAuteur(auteur);
        rapport.setModele(request.getModele());
        rapport.setNom(request.getNom());
        rapport.setTitre(request.getTitre());
        rapport.setContenu(versTexte(request));

        RapportDiffuse rapportEnregistre = rapportDiffuseRepository.save(rapport);
        RapportDiffuseResponse reponse = versReponse(rapportEnregistre);
        log.info("Rapport {} ({}) diffuse sur le projet {} par l'utilisateur {}", reponse.getId(), rapport.getModele(), projet.getId(), auteur.getId());
        notificationService.notifierDiffusionRapport(rapportEnregistre);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RapportDiffuseResponse> rechercherPourUtilisateurCourant() {
        Utilisateur utilisateur = utilisateurCourantService.obtenirUtilisateurCourant();
        boolean directeur = utilisateur.getRole() != null && ROLE_DIRECTEUR_DSI.equals(utilisateur.getRole().getNom());
        Map<Long, Boolean> accesParProjet = new HashMap<>();
        return rapportDiffuseRepository.findAllByOrderByDateDiffusionDesc().stream()
                .filter(rapport -> directeur || accesParProjet.computeIfAbsent(
                        rapport.getProjet().getId(), id -> estConcerne(rapport.getProjet(), utilisateur)))
                .map(this::versReponse)
                .collect(Collectors.toList());
    }

    /** Membre concerne : responsable, chef ou membre de l'equipe du projet, ou assigne a une tache du projet. */
    private boolean estConcerne(Projet projet, Utilisateur utilisateur) {
        if (estResponsable(projet, utilisateur)) {
            return true;
        }
        if (projet.getEquipe() != null) {
            if (projet.getEquipe().getChefEquipe() != null && projet.getEquipe().getChefEquipe().getId().equals(utilisateur.getId())) {
                return true;
            }
            if (membreEquipeRepository.existsByEquipeIdAndUtilisateurId(projet.getEquipe().getId(), utilisateur.getId())) {
                return true;
            }
        }
        return affectationTacheRepository.existsByTacheProjetIdAndUtilisateurId(projet.getId(), utilisateur.getId());
    }

    private boolean estResponsable(Projet projet, Utilisateur utilisateur) {
        return projet.getResponsable() != null && projet.getResponsable().getId().equals(utilisateur.getId());
    }

    private String versTexte(RapportDiffuseRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getContenu());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Contenu du rapport illisible.", e);
        }
    }

    private RapportDiffuseResponse versReponse(RapportDiffuse rapport) {
        RapportDiffuseResponse reponse = rapportDiffuseMapper.toResponse(rapport);
        try {
            reponse.setContenu(objectMapper.readTree(rapport.getContenu()));
        } catch (JsonProcessingException e) {
            log.warn("Contenu du rapport diffuse {} illisible : {}", rapport.getId(), e.getMessage());
        }
        return reponse;
    }
}
