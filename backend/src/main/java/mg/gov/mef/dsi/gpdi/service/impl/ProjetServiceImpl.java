package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.ProjetRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ProjetResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ClotureProjetImpossibleException;
import mg.gov.mef.dsi.gpdi.exception.EquipeIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutProjetInvalideException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonEligibleException;
import mg.gov.mef.dsi.gpdi.mapper.ProjetMapper;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import mg.gov.mef.dsi.gpdi.service.ProjetService;
import mg.gov.mef.dsi.gpdi.util.ProgressionProjet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjetServiceImpl implements ProjetService {

    private static final Logger log = LoggerFactory.getLogger(ProjetServiceImpl.class);

    private static final String ROLE_RESPONSABLE = "chef_projet";

    /** Projet en cours dont la date de fin est deja depassee — indicateur de sante uniquement, ne change pas le statut. */
    private static final int JOURS_SEUIL_SOUS_SURVEILLANCE = 14;

    private static final Set<StatutProjet> STATUTS_MODIFIABLES_LIBREMENT =
            EnumSet.of(StatutProjet.EN_PREPARATION, StatutProjet.EN_COURS);

    private static final Set<StatutProjet> STATUTS_TERMINAUX =
            EnumSet.of(StatutProjet.CLOTURE, StatutProjet.ARCHIVE);

    /** Un jalon dans l'un de ces statuts est encore "en cours" : il bloque la cloture du projet. */
    private static final Set<StatutJalon> STATUTS_JALON_BLOQUANT_CLOTURE =
            EnumSet.of(StatutJalon.A_VENIR, StatutJalon.EN_RETARD);

    private final ProjetRepository projetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EquipeRepository equipeRepository;
    private final ProjetMapper projetMapper;
    private final HabilitationService habilitationService;
    private final TacheRepository tacheRepository;
    private final SprintRepository sprintRepository;
    private final JalonRepository jalonRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ProjetResponse creer(ProjetRequest request) {
        habilitationService.verifierEcriture("Création des projets");
        Utilisateur responsable = obtenirResponsable(request.getIdResponsable());
        Equipe equipe = obtenirEquipe(request.getIdEquipe());

        Projet projet = projetMapper.toEntity(request);
        projet.setResponsable(responsable);
        projet.setEquipe(equipe);
        projet.setStatut(request.getStatut() != null ? validerStatutLibre(request.getStatut()) : StatutProjet.EN_PREPARATION);
        activerEquipeSiProjetEnCours(projet);

        projet = projetRepository.save(projet);
        projet.setCodeReference(genererCodeReference(projet));
        projet = projetRepository.save(projet);

        ProjetResponse reponse = versReponseAvecSante(projet);
        log.info("Projet {} cree", reponse.getId());
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjetResponse> rechercher(Pageable pageable) {
        return projetRepository.findAll(pageable).map(this::versReponseAvecSante);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjetResponse obtenir(Long id) {
        return projetRepository.findById(id)
                .map(this::versReponseAvecSante)
                .orElseThrow(() -> new ProjetIntrouvableException(id));
    }

    @Override
    @Transactional
    public ProjetResponse modifier(Long id, ProjetRequest request) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ProjetIntrouvableException(id));
        Utilisateur responsable = obtenirResponsable(request.getIdResponsable());

        projetMapper.updateEntity(request, projet);
        projet.setResponsable(responsable);
        // L'equipe n'est reassignee que si explicitement fournie : un formulaire d'edition qui ne
        // gere pas encore ce champ ne doit jamais desassigner l'equipe existante du projet.
        if (request.getIdEquipe() != null) {
            projet.setEquipe(obtenirEquipe(request.getIdEquipe()));
        }
        // Le statut n'est reinitialise que si explicitement fourni : une simple edition des
        // informations du projet ne doit jamais faire revenir son cycle de vie en arriere.
        if (request.getStatut() != null) {
            projet.setStatut(validerStatutLibre(request.getStatut()));
        }
        activerEquipeSiProjetEnCours(projet);

        ProjetResponse reponse = versReponseAvecSante(projetRepository.save(projet));
        log.info("Projet {} modifie", id);
        return reponse;
    }

    @Override
    @Transactional
    public ProjetResponse demarrer(Long id) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ProjetIntrouvableException(id));
        if (projet.getStatut() != StatutProjet.EN_PREPARATION) {
            throw new StatutProjetInvalideException(id, projet.getStatut(), "demarre");
        }
        projet.setStatut(StatutProjet.EN_COURS);
        activerEquipeSiProjetEnCours(projet);

        ProjetResponse reponse = versReponseAvecSante(projetRepository.save(projet));
        log.info("Projet {} demarre manuellement", id);
        notificationService.notifierDemarrageProjet(projet);
        return reponse;
    }

    @Override
    @Transactional
    public ProjetResponse reprendre(Long id) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ProjetIntrouvableException(id));
        if (projet.getStatut() != StatutProjet.SUSPENDU) {
            throw new StatutProjetInvalideException(id, projet.getStatut(), "repris");
        }
        projet.setStatut(StatutProjet.EN_COURS);
        activerEquipeSiProjetEnCours(projet);

        ProjetResponse reponse = versReponseAvecSante(projetRepository.save(projet));
        log.info("Projet {} repris", id);
        notificationService.notifierRepriseProjet(projet);
        return reponse;
    }

    @Override
    @Transactional
    public ProjetResponse suspendre(Long id) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ProjetIntrouvableException(id));
        projet.setStatut(StatutProjet.SUSPENDU);

        ProjetResponse reponse = versReponseAvecSante(projetRepository.save(projet));
        log.info("Projet {} suspendu", id);
        notificationService.notifierSuspensionProjet(projet);
        return reponse;
    }

    @Override
    @Transactional
    public ProjetResponse cloturer(Long id) {
        habilitationService.verifierEcriture("Clôture du projet");
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ProjetIntrouvableException(id));
        verifierProjetCloturable(id);
        projet.setStatut(StatutProjet.CLOTURE);
        projet.setDateCloture(LocalDate.now());

        ProjetResponse reponse = versReponseAvecSante(projetRepository.save(projet));
        log.info("Projet {} cloture", id);
        notificationService.notifierClotureProjet(projet);
        return reponse;
    }

    /**
     * Un projet ne peut etre cloture que si tous ses sprints sont CLOTURER et tous ses jalons sont
     * ATTEINT ou ANNULER (plus aucun A_VENIR ni EN_RETARD).
     */
    private void verifierProjetCloturable(Long idProjet) {
        boolean sprintEnCours = sprintRepository.findByProjetId(idProjet).stream()
                .anyMatch(sprint -> sprint.getStatut() != StatutSprint.CLOTURER);
        boolean jalonEnCours = jalonRepository.findByProjetId(idProjet).stream()
                .anyMatch(jalon -> STATUTS_JALON_BLOQUANT_CLOTURE.contains(jalon.getStatut()));
        if (sprintEnCours || jalonEnCours) {
            throw new ClotureProjetImpossibleException();
        }
    }

    @Override
    @Transactional
    public ProjetResponse archiver(Long id) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ProjetIntrouvableException(id));
        projet.setStatut(StatutProjet.ARCHIVE);
        projet.setArchive(true);

        ProjetResponse reponse = versReponseAvecSante(projetRepository.save(projet));
        log.info("Projet {} archive", id);
        return reponse;
    }

    @Override
    @Transactional
    public void demarrerLesProjetsArrivesADateDeDebut() {
        List<Projet> projets = projetRepository.findByStatutAndDateDebutLessThanEqual(
                StatutProjet.EN_PREPARATION, LocalDate.now());
        for (Projet projet : projets) {
            projet.setStatut(StatutProjet.EN_COURS);
            activerEquipeSiProjetEnCours(projet);
            projetRepository.save(projet);
            log.info("Projet {} demarre automatiquement (date de debut atteinte)", projet.getId());
            notificationService.notifierDemarrageProjet(projet);
        }
    }

    private String genererCodeReference(Projet projet) {
        int annee = projet.getDateDebut() != null ? projet.getDateDebut().getYear() : LocalDate.now().getYear();
        return String.format("GPDI-%d-%03d", annee, projet.getId());
    }

    private ProjetResponse versReponseAvecSante(Projet projet) {
        ProjetResponse reponse = projetMapper.toResponse(projet);
        reponse.setSante(calculerSante(projet));
        reponse.setProgression(calculerProgression(projet));
        return reponse;
    }

    /** Progression calculee a la lecture (cf. ProgressionProjet) ; 0 si le projet n'a aucune tache. */
    private int calculerProgression(Projet projet) {
        if (projet.getId() == null) {
            return 0;
        }
        long total = tacheRepository.countByProjetId(projet.getId());
        if (total == 0) {
            return 0;
        }
        long terminees = tacheRepository.countByProjetIdAndStatut(projet.getId(), StatutTache.TERMINEE);
        return ProgressionProjet.calculer(terminees, total);
    }

    private String calculerSante(Projet projet) {
        if (STATUTS_TERMINAUX.contains(projet.getStatut()) || projet.getStatut() == StatutProjet.SUSPENDU) {
            return "sain";
        }
        if (projet.getDateFin() == null) {
            return "sain";
        }
        LocalDate aujourdhui = LocalDate.now();
        if (projet.getDateFin().isBefore(aujourdhui)) {
            return "a_risque";
        }
        if (!projet.getDateFin().isAfter(aujourdhui.plusDays(JOURS_SEUIL_SOUS_SURVEILLANCE))) {
            return "sous_surveillance";
        }
        return "sain";
    }

    private Utilisateur obtenirResponsable(Long idResponsable) {
        Utilisateur utilisateur = utilisateurRepository.findById(idResponsable)
                .orElseThrow(() -> new UtilisateurIntrouvableException(idResponsable));
        if (utilisateur.getRole() == null || !ROLE_RESPONSABLE.equals(utilisateur.getRole().getNom())) {
            throw new UtilisateurNonEligibleException(idResponsable, ROLE_RESPONSABLE);
        }
        return utilisateur;
    }

    private Equipe obtenirEquipe(Long idEquipe) {
        if (idEquipe == null) {
            return null;
        }
        return equipeRepository.findById(idEquipe)
                .orElseThrow(() -> new EquipeIntrouvableException(idEquipe));
    }

    private void activerEquipeSiProjetEnCours(Projet projet) {
        if (projet.getStatut() == StatutProjet.EN_COURS
                && projet.getEquipe() != null
                && !Boolean.TRUE.equals(projet.getEquipe().getActive())) {
            projet.getEquipe().setActive(true);
            equipeRepository.save(projet.getEquipe());
        }
    }

    private StatutProjet validerStatutLibre(StatutProjet statut) {
        if (statut == null) {
            return StatutProjet.EN_PREPARATION;
        }
        if (!STATUTS_MODIFIABLES_LIBREMENT.contains(statut)) {
            throw new StatutProjetInvalideException(statut);
        }
        return statut;
    }
}
