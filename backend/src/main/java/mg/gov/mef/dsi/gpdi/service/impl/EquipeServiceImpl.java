package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.EquipeRequest;
import mg.gov.mef.dsi.gpdi.dto.response.EquipeResponse;
import mg.gov.mef.dsi.gpdi.dto.response.MembreEquipeResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.EquipeIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.EquipeRattacheeProjetException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonEligibleException;
import mg.gov.mef.dsi.gpdi.mapper.EquipeMapper;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.EquipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipeServiceImpl implements EquipeService {

    private static final Logger log = LoggerFactory.getLogger(EquipeServiceImpl.class);

    /**
     * Le "chef d'équipe" n'est pas un rôle applicatif à part (pas de login dédié) : c'est une
     * désignation portée par un membre d'équipe ou un chef de projet pour la durée de son mandat
     * sur une équipe donnée. Un chef de projet peut aussi faire partie d'une équipe DSI.
     */
    private static final Set<String> ROLES_ELIGIBLES = new LinkedHashSet<>(
            Arrays.asList("membre_equipe", "chef_projet"));

    private final EquipeRepository equipeRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProjetRepository projetRepository;
    private final EquipeMapper equipeMapper;

    @Override
    @Transactional
    public EquipeResponse creer(EquipeRequest request) {
        Utilisateur chef = obtenirUtilisateurEligible(request.getIdChefEquipe());

        Equipe equipe = equipeMapper.toEntity(request);
        equipe.setChefEquipe(chef);
        equipe = equipeRepository.save(equipe);

        List<MembreEquipeResponse> membres = ajouterMembres(equipe, new LinkedHashSet<>(request.getIdsMembres()));

        EquipeResponse reponse = equipeMapper.toResponse(equipe);
        reponse.setMembres(membres);
        log.info("Equipe {} creee", reponse.getId());
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EquipeResponse> rechercher(Pageable pageable) {
        Page<Equipe> page = equipeRepository.findAll(pageable);
        List<Long> idsEquipes = page.getContent().stream().map(Equipe::getId).collect(Collectors.toList());

        Map<Long, List<MembreEquipeResponse>> membresParEquipe = membreEquipeRepository.findByEquipeIdIn(idsEquipes)
                .stream()
                .collect(Collectors.groupingBy(
                        membre -> membre.getEquipe().getId(),
                        Collectors.mapping(equipeMapper::toMembreResponse, Collectors.toList())));

        return page.map(equipe -> {
            EquipeResponse reponse = equipeMapper.toResponse(equipe);
            reponse.setMembres(membresParEquipe.getOrDefault(equipe.getId(), Collections.emptyList()));
            return reponse;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public EquipeResponse obtenir(Long id) {
        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new EquipeIntrouvableException(id));

        EquipeResponse reponse = equipeMapper.toResponse(equipe);
        reponse.setMembres(membreEquipeRepository.findByEquipeId(id).stream()
                .map(equipeMapper::toMembreResponse)
                .collect(Collectors.toList()));
        return reponse;
    }

    @Override
    @Transactional
    public EquipeResponse modifier(Long id, EquipeRequest request) {
        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new EquipeIntrouvableException(id));
        Utilisateur chef = obtenirUtilisateurEligible(request.getIdChefEquipe());

        equipeMapper.updateEntity(request, equipe);
        equipe.setChefEquipe(chef);
        equipeRepository.save(equipe);

        Set<Long> nouveauxIds = new LinkedHashSet<>(request.getIdsMembres());
        List<MembreEquipe> membresActuels = membreEquipeRepository.findByEquipeId(id);

        List<MembreEquipe> aSupprimer = membresActuels.stream()
                .filter(membre -> !nouveauxIds.contains(membre.getUtilisateur().getId()))
                .collect(Collectors.toList());
        membreEquipeRepository.deleteAll(aSupprimer);

        Set<Long> idsExistants = membresActuels.stream()
                .map(membre -> membre.getUtilisateur().getId())
                .collect(Collectors.toSet());
        Set<Long> idsAAjouter = nouveauxIds.stream()
                .filter(idMembre -> !idsExistants.contains(idMembre))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ajouterMembres(equipe, idsAAjouter);

        List<MembreEquipeResponse> membresFinal = membreEquipeRepository.findByEquipeId(id).stream()
                .map(equipeMapper::toMembreResponse)
                .collect(Collectors.toList());

        EquipeResponse reponse = equipeMapper.toResponse(equipe);
        reponse.setMembres(membresFinal);
        log.info("Equipe {} modifiee", id);
        return reponse;
    }

    @Override
    @Transactional
    public void supprimer(Long id) {
        if (!equipeRepository.existsById(id)) {
            throw new EquipeIntrouvableException(id);
        }
        if (projetRepository.existsByEquipeId(id)) {
            throw new EquipeRattacheeProjetException(id);
        }
        membreEquipeRepository.deleteByEquipeId(id);
        equipeRepository.deleteById(id);
        log.info("Equipe {} supprimee", id);
    }

    private List<MembreEquipeResponse> ajouterMembres(Equipe equipe, Set<Long> idsMembres) {
        return idsMembres.stream()
                .map(idMembre -> {
                    Utilisateur utilisateur = obtenirUtilisateurEligible(idMembre);
                    MembreEquipe membreEquipe = new MembreEquipe();
                    membreEquipe.setEquipe(equipe);
                    membreEquipe.setUtilisateur(utilisateur);
                    return equipeMapper.toMembreResponse(membreEquipeRepository.save(membreEquipe));
                })
                .collect(Collectors.toList());
    }

    private Utilisateur obtenirUtilisateurEligible(Long idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new UtilisateurIntrouvableException(idUtilisateur));
        if (utilisateur.getRole() == null || !ROLES_ELIGIBLES.contains(utilisateur.getRole().getNom())) {
            throw new UtilisateurNonEligibleException(idUtilisateur, String.join(", ", ROLES_ELIGIBLES));
        }
        return utilisateur;
    }
}
