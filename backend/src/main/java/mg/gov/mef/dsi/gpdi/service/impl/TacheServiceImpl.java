package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TacheDetailResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurAssigneResponse;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TachePrecedence;
import mg.gov.mef.dsi.gpdi.entity.TypeLien;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.DateFinTacheInvalideException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SprintIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SprintProjetIncoherentException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceInvalideException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonMembreEquipeException;
import mg.gov.mef.dsi.gpdi.mapper.TacheMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.CommentaireRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.SousTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TachePrecedenceRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import mg.gov.mef.dsi.gpdi.service.TacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TacheServiceImpl implements TacheService {

    private static final Logger log = LoggerFactory.getLogger(TacheServiceImpl.class);

    private final TacheRepository tacheRepository;
    private final ProjetRepository projetRepository;
    private final SprintRepository sprintRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MembreEquipeRepository membreEquipeRepository;
    private final AffectationTacheRepository affectationTacheRepository;
    private final SousTacheRepository sousTacheRepository;
    private final CommentaireRepository commentaireRepository;
    private final TachePrecedenceRepository tachePrecedenceRepository;
    private final TacheMapper tacheMapper;
    private final NotificationService notificationService;
    private final AccesProjetService accesProjetService;
    private final HabilitationService habilitationService;

    @Override
    @Transactional
    public TacheResponse creer(Long idProjet, TacheRequest request) {
        Projet projet = obtenirProjet(idProjet);
        accesProjetService.verifierMembreDuProjet(projet);
        habilitationService.verifierEcriture("Création des tâches");
        Sprint sprint = obtenirSprintCoherent(idProjet, request.getIdSprint());
        List<Utilisateur> assignes = obtenirUtilisateursEligibles(projet, request.getIdsUtilisateursAssignes());

        Tache tache = tacheMapper.toEntity(request);
        tache.setProjet(projet);
        tache.setSprint(sprint);
        if (request.getTempsPasse() != null) {
            tache.setTempsPasse(request.getTempsPasse());
        }
        if (request.getStatut() != null) {
            appliquerChangementDeStatut(tache, request.getStatut());
        }
        tache = tacheRepository.save(tache);

        tache.setCode("T-" + tache.getId());
        tache = tacheRepository.save(tache);

        List<UtilisateurAssigneResponse> assignations = remplacerAssignations(tache, assignes);

        TacheResponse reponse = tacheMapper.toResponse(tache);
        reponse.setAssignations(assignations);
        log.info("Tache {} creee pour le projet {}", reponse.getCode(), idProjet);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TacheResponse> rechercher(Long idProjet, Pageable pageable) {
        Projet projet = obtenirProjet(idProjet);
        accesProjetService.verifierAccesLecture(projet);
        Page<Tache> page = tacheRepository.findByProjetId(idProjet, pageable);
        List<Long> idsTaches = page.getContent().stream().map(Tache::getId).collect(Collectors.toList());

        Map<Long, List<UtilisateurAssigneResponse>> assignationsParTache = affectationTacheRepository
                .findByTacheIdIn(idsTaches)
                .stream()
                .collect(Collectors.groupingBy(
                        affectation -> affectation.getTache().getId(),
                        Collectors.mapping(tacheMapper::toAssignationResponse, Collectors.toList())));

        return page.map(tache -> {
            TacheResponse reponse = tacheMapper.toResponse(tache);
            reponse.setAssignations(assignationsParTache.getOrDefault(tache.getId(), Collections.emptyList()));
            return reponse;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TacheDetailResponse obtenir(Long idProjet, Long id) {
        Tache tache = obtenirTacheDuProjet(idProjet, id);
        accesProjetService.verifierAccesLecture(tache.getProjet());

        TacheDetailResponse reponse = tacheMapper.toDetailResponse(tache);
        reponse.setAssignations(affectationTacheRepository.findByTacheId(id).stream()
                .map(tacheMapper::toAssignationResponse)
                .collect(Collectors.toList()));
        reponse.setSousTaches(sousTacheRepository.findByTacheId(id).stream()
                .map(tacheMapper::toSousTacheResponse)
                .collect(Collectors.toList()));
        reponse.setDependances(tachePrecedenceRepository.findByTacheSourceIdOrTacheCibleId(id, id).stream()
                .map(tacheMapper::toPrecedenceResponse)
                .collect(Collectors.toList()));
        reponse.setCommentaires(commentaireRepository.findByTacheIdOrderByDateCreationAsc(id).stream()
                .map(tacheMapper::toCommentaireResponse)
                .collect(Collectors.toList()));
        return reponse;
    }

    @Override
    @Transactional
    public TacheResponse modifier(Long idProjet, Long id, TacheRequest request) {
        Tache tache = obtenirTacheDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(tache.getProjet());
        verifierHabilitationsChampsModifies(tache, request);
        Sprint sprint = obtenirSprintCoherent(idProjet, request.getIdSprint());
        List<Utilisateur> assignes = obtenirUtilisateursEligibles(tache.getProjet(), request.getIdsUtilisateursAssignes());

        StatutTache ancienStatut = tache.getStatut();
        tacheMapper.updateEntity(request, tache);
        tache.setSprint(sprint);
        // Le temps passe n'est ecrase que si explicitement fourni (saisie manuelle) : toute autre
        // modification de la tache doit conserver le temps deja enregistre.
        if (request.getTempsPasse() != null) {
            tache.setTempsPasse(request.getTempsPasse());
        }
        // Le statut n'est reinitialise que si explicitement fourni : deplacer une tache entre
        // sprints ou modifier son titre ne doit jamais faire revenir son statut en arriere.
        if (request.getStatut() != null) {
            appliquerChangementDeStatut(tache, request.getStatut());
        }
        tache = tacheRepository.save(tache);

        List<UtilisateurAssigneResponse> assignations = remplacerAssignations(tache, assignes);

        if (request.getStatut() == StatutTache.BLOQUEE && ancienStatut != StatutTache.BLOQUEE) {
            notificationService.notifierBlocage(tache, assignes);
        }

        TacheResponse reponse = tacheMapper.toResponse(tache);
        reponse.setAssignations(assignations);
        log.info("Tache {} modifiee", id);
        return reponse;
    }

    @Override
    @Transactional
    public void supprimer(Long idProjet, Long id) {
        Tache tache = obtenirTacheDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(tache.getProjet());
        affectationTacheRepository.deleteByTacheId(id);
        sousTacheRepository.deleteByTacheId(id);
        commentaireRepository.deleteByTacheId(id);
        tachePrecedenceRepository.deleteByTacheSourceIdOrTacheCibleId(id, id);
        tacheRepository.delete(tache);
        log.info("Tache {} supprimee", id);
    }

    /**
     * Demarre automatiquement le chronometre de suivi du temps au passage a EN_COURS (si pas deja
     * demarre), et le fige/arrete au passage a TERMINEE (le temps ecoule est ajoute a tempsPasse).
     * Les autres transitions (y compris un retour a EN_COURS depuis un autre statut non terminal)
     * ne touchent jamais un chronometre deja lance.
     * <p>
     * Tant qu'une tache n'a jamais demarre (statut A_FAIRE ou REPORTEE), en sortir — vers EN_COURS,
     * EN_TEST, BLOQUEE ou TERMINEE — est bloque si une dependance de demarrage (FIN_DEBUT/DEBUT_DEBUT)
     * n'est pas encore satisfaite : la tache reste figee en A_FAIRE jusqu'a ce que la condition le soit.
     * Le passage a TERMINEE est en plus bloque si une dependance de cloture (FIN_FIN/DEBUT_FIN) n'est
     * pas satisfaite.
     */
    private void appliquerChangementDeStatut(Tache tache, StatutTache nouveauStatut) {
        boolean pasEncoreDemarree = tache.getStatut() == StatutTache.A_FAIRE || tache.getStatut() == StatutTache.REPORTEE;
        if (pasEncoreDemarree && nouveauStatut != tache.getStatut()) {
            verifierConditionDemarrage(tache);
        }
        if (nouveauStatut == StatutTache.EN_COURS) {
            if (tache.getDemarreeLe() == null) {
                tache.setDemarreeLe(LocalDateTime.now());
            }
        } else if (nouveauStatut == StatutTache.TERMINEE) {
            verifierConditionCloture(tache);
            LocalDate dateFinReelle = LocalDate.now();
            verifierDateFinDansLeSprint(tache, dateFinReelle);
            if (tache.getDemarreeLe() != null) {
                double heuresEcoulees = Duration.between(tache.getDemarreeLe(), LocalDateTime.now()).toMinutes() / 60.0;
                double tempsPasseActuel = tache.getTempsPasse() != null ? tache.getTempsPasse() : 0.0;
                tache.setTempsPasse(tempsPasseActuel + heuresEcoulees);
                tache.setDemarreeLe(null);
                tache.setDateFinReelle(dateFinReelle);
            }
        }
        tache.setStatut(nouveauStatut);
    }

    /** La date de cloture d'une tache ne peut pas depasser la date de cloture du sprint auquel elle appartient (si elle en a un). */
    private void verifierDateFinDansLeSprint(Tache tache, LocalDate dateFinReelle) {
        Sprint sprint = tache.getSprint();
        if (sprint != null && sprint.getDateFin() != null && dateFinReelle.isAfter(sprint.getDateFin())) {
            throw new DateFinTacheInvalideException(dateFinReelle, sprint.getDateFin());
        }
    }

    private void verifierConditionDemarrage(Tache tache) {
        for (TachePrecedence precedence : dependancesEntrantes(tache)) {
            Tache source = precedence.getTacheSource();
            if (precedence.getTypeLien() == TypeLien.FIN_DEBUT && source.getStatut() != StatutTache.TERMINEE) {
                throw new TachePrecedenceInvalideException(
                        "Impossible de demarrer cette tache : la tache " + source.getCode() + " doit d'abord etre terminee.");
            }
            if (precedence.getTypeLien() == TypeLien.DEBUT_DEBUT && !aDemarre(source)) {
                throw new TachePrecedenceInvalideException(
                        "Impossible de demarrer cette tache : la tache " + source.getCode() + " doit d'abord etre demarree.");
            }
        }
    }

    private void verifierConditionCloture(Tache tache) {
        for (TachePrecedence precedence : dependancesEntrantes(tache)) {
            Tache source = precedence.getTacheSource();
            if (precedence.getTypeLien() == TypeLien.FIN_FIN && source.getStatut() != StatutTache.TERMINEE) {
                throw new TachePrecedenceInvalideException(
                        "Impossible de terminer cette tache : la tache " + source.getCode() + " doit d'abord etre terminee.");
            }
            if (precedence.getTypeLien() == TypeLien.DEBUT_FIN && !aDemarre(source)) {
                throw new TachePrecedenceInvalideException(
                        "Impossible de terminer cette tache : la tache " + source.getCode() + " doit d'abord etre demarree.");
            }
        }
    }

    /** Dependances ou cette tache est la "cible" (celle qui attend). Une tache toute juste creee (id encore null) n'en a aucune. */
    private List<TachePrecedence> dependancesEntrantes(Tache tache) {
        if (tache.getId() == null) {
            return Collections.emptyList();
        }
        return tachePrecedenceRepository.findByTacheCibleId(tache.getId());
    }

    private boolean aDemarre(Tache tache) {
        return tache.getStatut() == StatutTache.TERMINEE
                || tache.getDemarreeLe() != null
                || (tache.getTempsPasse() != null && tache.getTempsPasse() > 0);
    }

    private List<UtilisateurAssigneResponse> remplacerAssignations(Tache tache, List<Utilisateur> assignes) {
        affectationTacheRepository.deleteByTacheId(tache.getId());
        return assignes.stream()
                .map(utilisateur -> {
                    AffectationTache affectation = new AffectationTache();
                    affectation.setTache(tache);
                    affectation.setUtilisateur(utilisateur);
                    return tacheMapper.toAssignationResponse(affectationTacheRepository.save(affectation));
                })
                .collect(Collectors.toList());
    }

    private Projet obtenirProjet(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
    }

    private Sprint obtenirSprintCoherent(Long idProjet, Long idSprint) {
        if (idSprint == null) {
            return null;
        }
        Sprint sprint = sprintRepository.findById(idSprint)
                .orElseThrow(() -> new SprintIntrouvableException(idSprint));
        if (sprint.getProjet() == null || !sprint.getProjet().getId().equals(idProjet)) {
            throw new SprintProjetIncoherentException(idSprint, idProjet);
        }
        return sprint;
    }

    private List<Utilisateur> obtenirUtilisateursEligibles(Projet projet, List<Long> idsUtilisateurs) {
        if (idsUtilisateurs == null || idsUtilisateurs.isEmpty()) {
            return Collections.emptyList();
        }
        Long idEquipe = projet.getEquipe() != null ? projet.getEquipe().getId() : null;
        return idsUtilisateurs.stream()
                .map(idUtilisateur -> {
                    Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                            .orElseThrow(() -> new UtilisateurIntrouvableException(idUtilisateur));
                    if (idEquipe == null || !membreEquipeRepository.existsByEquipeIdAndUtilisateurId(idEquipe, idUtilisateur)) {
                        throw new UtilisateurNonMembreEquipeException(idUtilisateur);
                    }
                    return utilisateur;
                })
                .collect(Collectors.toList());
    }

    /**
     * RBAC dynamique (matrice d'habilitations) : la ligne "Changement de statut" de la matrice donne au
     * membre d'equipe un droit d'ecriture plus restreint que le chef de projet (statut/temps passe
     * uniquement, pas le titre/priorite/assignation). Le endpoint PUT reste un remplacement complet en un
     * seul appel (contrat frontend inchange) ; seuls les champs reellement modifies sont donc verifies,
     * chacun contre la fonction de la matrice qui le gouverne.
     */
    private void verifierHabilitationsChampsModifies(Tache tache, TacheRequest request) {
        Set<String> fonctionsAVerifier = new LinkedHashSet<>();

        if (request.getStatut() != null && request.getStatut() != tache.getStatut()) {
            fonctionsAVerifier.add("Changement de statut");
        }
        if (request.getTempsPasse() != null && !request.getTempsPasse().equals(tache.getTempsPasse())) {
            fonctionsAVerifier.add("Enregistrement du temps passé");
        }
        if (request.getPriorite() != null && request.getPriorite() != tache.getPriorite()) {
            fonctionsAVerifier.add("Priorisation des tâches");
        }
        Set<Long> assignesActuels = affectationTacheRepository.findByTacheId(tache.getId()).stream()
                .map(affectation -> affectation.getUtilisateur().getId())
                .collect(Collectors.toSet());
        Set<Long> assignesDemandes = request.getIdsUtilisateursAssignes() == null
                ? Collections.emptySet() : new HashSet<>(request.getIdsUtilisateursAssignes());
        if (!assignesActuels.equals(assignesDemandes)) {
            fonctionsAVerifier.add("Affectation des tâches");
        }
        boolean autresChamps = !Objects.equals(request.getTitre(), tache.getTitre())
                || !Objects.equals(request.getDescription(), tache.getDescription())
                || !Objects.equals(request.getTempsEstime(), tache.getTempsEstime())
                || !Objects.equals(request.getDateEcheance(), tache.getDateFinPrevue())
                || !Objects.equals(request.getIdSprint(), tache.getSprint() != null ? tache.getSprint().getId() : null);
        if (autresChamps) {
            fonctionsAVerifier.add("Création des tâches");
        }

        fonctionsAVerifier.forEach(habilitationService::verifierEcriture);
    }

    private Tache obtenirTacheDuProjet(Long idProjet, Long id) {
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new TacheIntrouvableException(id));
        if (tache.getProjet() == null || !tache.getProjet().getId().equals(idProjet)) {
            throw new TacheIntrouvableException(id);
        }
        return tache;
    }
}
