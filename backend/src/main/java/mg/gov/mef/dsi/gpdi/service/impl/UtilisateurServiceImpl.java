package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.client.KeycloakAdminClient;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurCreationRequest;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurModificationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurResponse;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.CompteSuspenduException;
import mg.gov.mef.dsi.gpdi.exception.RoleIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurAssigneATachesException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.UtilisateurMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private static final Logger log = LoggerFactory.getLogger(UtilisateurServiceImpl.class);

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurCourantService utilisateurCourantService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AffectationTacheRepository affectationTacheRepository;

    @Override
    @Transactional
    public UtilisateurResponse creer(UtilisateurCreationRequest request) {
        Role role = obtenirRole(request.getIdRole());

        Utilisateur utilisateur = utilisateurMapper.toEntity(request);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setRole(role);
        utilisateur = utilisateurRepository.save(utilisateur);

        // Si le provisioning Keycloak echoue, l'exception remonte et la transaction annule
        // l'enregistrement Postgres ci-dessus (cf. KeycloakAdminClient.creerUtilisateur).
        keycloakAdminClient.creerUtilisateur(request.getMatricule(), request.getMotDePasse(),
                request.getEmail(), request.getPrenom(), request.getNom());

        UtilisateurResponse reponse = utilisateurMapper.toResponse(utilisateur);
        log.info("Utilisateur {} cree", reponse.getId());
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UtilisateurResponse> rechercher(Pageable pageable) {
        return utilisateurRepository.findAll(pageable).map(utilisateurMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponse obtenir(Long id) {
        return utilisateurRepository.findById(id)
                .map(utilisateurMapper::toResponse)
                .orElseThrow(() -> new UtilisateurIntrouvableException(id));
    }

    @Override
    @Transactional
    public UtilisateurResponse modifier(Long id, UtilisateurModificationRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UtilisateurIntrouvableException(id));
        Role role = obtenirRole(request.getIdRole());

        utilisateurMapper.updateEntity(request, utilisateur);
        utilisateur.setRole(role);
        if (StringUtils.hasText(request.getMotDePasse())) {
            utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        }

        UtilisateurResponse reponse = utilisateurMapper.toResponse(utilisateurRepository.save(utilisateur));
        log.info("Utilisateur {} modifie", id);
        return reponse;
    }

    @Override
    @Transactional
    public void supprimer(Long id, boolean detacherTaches) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UtilisateurIntrouvableException(id));
        if (affectationTacheRepository.existsByUtilisateurId(id)) {
            if (!detacherTaches) {
                throw new UtilisateurAssigneATachesException(id);
            }
            affectationTacheRepository.deleteByUtilisateurId(id);
        }
        utilisateurRepository.deleteById(id);
        keycloakAdminClient.supprimerUtilisateur(utilisateur.getMatricule());
        log.info("Utilisateur {} supprime", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponse obtenirCourant() {
        Utilisateur utilisateur = utilisateurCourantService.obtenirUtilisateurCourant();
        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new CompteSuspenduException();
        }
        return utilisateurMapper.toResponse(utilisateur);
    }

    @Override
    @Transactional
    public UtilisateurResponse activer(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UtilisateurIntrouvableException(id));
        utilisateur.setActif(true);
        UtilisateurResponse reponse = utilisateurMapper.toResponse(utilisateurRepository.save(utilisateur));
        log.info("Utilisateur {} active", id);
        return reponse;
    }

    @Override
    @Transactional
    public UtilisateurResponse suspendre(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new UtilisateurIntrouvableException(id));
        utilisateur.setActif(false);
        UtilisateurResponse reponse = utilisateurMapper.toResponse(utilisateurRepository.save(utilisateur));
        log.info("Utilisateur {} suspendu", id);
        return reponse;
    }

    private Role obtenirRole(Long idRole) {
        return roleRepository.findById(idRole)
                .orElseThrow(() -> new RoleIntrouvableException(idRole));
    }
}
