package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.SprintRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SprintResponse;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SprintChevauchementException;
import mg.gov.mef.dsi.gpdi.exception.SprintIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutSprintInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.SprintMapper;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import mg.gov.mef.dsi.gpdi.service.SprintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private static final Logger log = LoggerFactory.getLogger(SprintServiceImpl.class);

    private static final String FONCTION_GESTION_SPRINTS = "Gestion des sprints";
    private static final String FONCTION_CLOTURE_SPRINT = "Clôture du sprint";

    private final SprintRepository sprintRepository;
    private final ProjetRepository projetRepository;
    private final SprintMapper sprintMapper;
    private final AccesProjetService accesProjetService;
    private final HabilitationService habilitationService;

    @Override
    @Transactional
    public SprintResponse creer(Long idProjet, SprintRequest request) {
        Projet projet = obtenirProjet(idProjet);
        accesProjetService.verifierMembreDuProjet(projet);
        habilitationService.verifierEcriture(FONCTION_GESTION_SPRINTS);
        verifierAbsenceChevauchement(idProjet, request.getDateDebut(), request.getDateFin(), null);

        Sprint sprint = sprintMapper.toEntity(request);
        sprint.setProjet(projet);

        SprintResponse reponse = sprintMapper.toResponse(sprintRepository.save(sprint));
        log.info("Sprint {} cree pour le projet {}", reponse.getId(), idProjet);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SprintResponse> rechercher(Long idProjet, Pageable pageable) {
        Projet projet = obtenirProjet(idProjet);
        accesProjetService.verifierAccesLecture(projet);
        return sprintRepository.findByProjetId(idProjet, pageable).map(sprintMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponse obtenir(Long idProjet, Long id) {
        Sprint sprint = obtenirSprintDuProjet(idProjet, id);
        accesProjetService.verifierAccesLecture(sprint.getProjet());
        return sprintMapper.toResponse(sprint);
    }

    @Override
    @Transactional
    public SprintResponse modifier(Long idProjet, Long id, SprintRequest request) {
        Sprint sprint = obtenirSprintDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(sprint.getProjet());
        habilitationService.verifierEcriture(FONCTION_GESTION_SPRINTS);
        verifierAbsenceChevauchement(idProjet, request.getDateDebut(), request.getDateFin(), id);
        sprintMapper.updateEntity(request, sprint);

        SprintResponse reponse = sprintMapper.toResponse(sprintRepository.save(sprint));
        log.info("Sprint {} modifie", id);
        return reponse;
    }

    @Override
    @Transactional
    public void supprimer(Long idProjet, Long id) {
        Sprint sprint = obtenirSprintDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(sprint.getProjet());
        habilitationService.verifierEcriture(FONCTION_GESTION_SPRINTS);
        sprintRepository.delete(sprint);
        log.info("Sprint {} supprime", id);
    }

    @Override
    @Transactional
    public SprintResponse commencer(Long idProjet, Long id) {
        Sprint sprint = obtenirSprintDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(sprint.getProjet());
        habilitationService.verifierEcriture(FONCTION_GESTION_SPRINTS);
        if (sprint.getStatut() != StatutSprint.PLANIFIE) {
            throw new StatutSprintInvalideException(id, sprint.getStatut(), "demarre");
        }
        sprint.setStatut(StatutSprint.EN_COURS);

        SprintResponse reponse = sprintMapper.toResponse(sprintRepository.save(sprint));
        log.info("Sprint {} demarre", id);
        return reponse;
    }

    @Override
    @Transactional
    public SprintResponse terminer(Long idProjet, Long id) {
        Sprint sprint = obtenirSprintDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(sprint.getProjet());
        habilitationService.verifierEcriture(FONCTION_CLOTURE_SPRINT);
        if (sprint.getStatut() != StatutSprint.EN_COURS) {
            throw new StatutSprintInvalideException(id, sprint.getStatut(), "cloture");
        }
        // La regle "toutes les taches doivent etre TERMINEE ou REPORTEE" sera appliquee ici
        // des que l'entite Tache existera. Sans tache en base, la condition est vide donc toujours vraie.
        sprint.setStatut(StatutSprint.CLOTURER);

        SprintResponse reponse = sprintMapper.toResponse(sprintRepository.save(sprint));
        log.info("Sprint {} cloture", id);
        return reponse;
    }

    private void verifierAbsenceChevauchement(Long idProjet, LocalDate dateDebut, LocalDate dateFin, Long idSprintExclu) {
        boolean chevauche = sprintRepository.findByProjetId(idProjet).stream()
                .filter(s -> idSprintExclu == null || !idSprintExclu.equals(s.getId()))
                .anyMatch(s -> !dateDebut.isAfter(s.getDateFin()) && !dateFin.isBefore(s.getDateDebut()));
        if (chevauche) {
            throw new SprintChevauchementException(dateDebut, dateFin);
        }
    }

    private Projet obtenirProjet(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
    }

    private Sprint obtenirSprintDuProjet(Long idProjet, Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new SprintIntrouvableException(id));
        if (sprint.getProjet() == null || !sprint.getProjet().getId().equals(idProjet)) {
            throw new SprintIntrouvableException(id);
        }
        return sprint;
    }
}
