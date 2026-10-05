package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.PartageClientRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PartageClientResponse;
import mg.gov.mef.dsi.gpdi.entity.PartageClient;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.PartageClientIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.PartageDejaActifException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutPartageInvalideException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonEligibleException;
import mg.gov.mef.dsi.gpdi.mapper.PartageClientMapper;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import mg.gov.mef.dsi.gpdi.service.PartageClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartageClientServiceImpl implements PartageClientService {

    private static final Logger log = LoggerFactory.getLogger(PartageClientServiceImpl.class);

    /** Seuls les comptes "Direction métier" (cf. core/models/role.model.ts côté frontend) peuvent recevoir un partage d'avancement. */
    private static final String ROLE_CLIENT = "direction_metier";

    private final PartageClientRepository partageClientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProjetRepository projetRepository;
    private final PartageClientMapper partageClientMapper;
    private final HabilitationService habilitationService;

    @Override
    @Transactional
    public List<PartageClientResponse> creer(PartageClientRequest request) {
        habilitationService.verifierEcriture("Partage de l'avancement au client");
        Utilisateur client = obtenirClientEligible(request.getIdUtilisateur());

        List<Projet> projets = request.getIdsProjets().stream()
                .map(idProjet -> projetRepository.findById(idProjet)
                        .orElseThrow(() -> new ProjetIntrouvableException(idProjet)))
                .collect(Collectors.toList());

        for (Projet projet : projets) {
            if (partageClientRepository.existsByClientIdAndProjetIdAndStatut(
                    client.getId(), projet.getId(), StatutPartage.ACTIF)) {
                throw new PartageDejaActifException(client.getId(), projet.getId());
            }
        }

        List<PartageClientResponse> reponses = new ArrayList<>();
        for (Projet projet : projets) {
            PartageClient partage = new PartageClient();
            partage.setClient(client);
            partage.setProjet(projet);
            partage.setDateEcheance(request.getDateEcheance());
            reponses.add(partageClientMapper.toResponse(partageClientRepository.save(partage)));
        }
        log.info("Projet(s) {} partage(s) avec le client {}", request.getIdsProjets(), client.getId());
        return reponses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartageClientResponse> rechercher(Pageable pageable) {
        return partageClientRepository.findAll(pageable).map(partageClientMapper::toResponse);
    }

    @Override
    @Transactional
    public PartageClientResponse revoquer(Long id) {
        PartageClient partage = partageClientRepository.findById(id)
                .orElseThrow(() -> new PartageClientIntrouvableException(id));
        if (partage.getStatut() != StatutPartage.ACTIF) {
            throw new StatutPartageInvalideException(id, partage.getStatut());
        }
        partage.setStatut(StatutPartage.REVOQUE);

        PartageClientResponse reponse = partageClientMapper.toResponse(partageClientRepository.save(partage));
        log.info("Partage {} revoque", id);
        return reponse;
    }

    @Override
    @Transactional
    public void expirerLesPartagesEnRetard() {
        List<PartageClient> partages = partageClientRepository.findByStatutAndDateEcheanceLessThan(
                StatutPartage.ACTIF, LocalDate.now());
        for (PartageClient partage : partages) {
            partage.setStatut(StatutPartage.EXPIRE);
            partageClientRepository.save(partage);
            log.info("Partage {} expire (echeance depassee)", partage.getId());
        }
    }

    private Utilisateur obtenirClientEligible(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new UtilisateurIntrouvableException(idUtilisateur));
        if (utilisateur.getRole() == null || !ROLE_CLIENT.equals(utilisateur.getRole().getNom())) {
            throw new UtilisateurNonEligibleException(idUtilisateur, ROLE_CLIENT);
        }
        return utilisateur;
    }
}
