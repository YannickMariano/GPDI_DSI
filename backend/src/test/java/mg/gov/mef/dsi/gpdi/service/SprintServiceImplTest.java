package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.SprintRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SprintResponse;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SprintChevauchementException;
import mg.gov.mef.dsi.gpdi.exception.SprintIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SprintPrecedentNonClotureException;
import mg.gov.mef.dsi.gpdi.exception.StatutSprintInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.SprintMapper;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.impl.SprintServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintServiceImplTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private SprintMapper sprintMapper;

    @Mock
    private AccesProjetService accesProjetService;

    @Mock
    private HabilitationService habilitationService;

    private SprintServiceImpl service;

    @BeforeEach
    void initService() {
        service = new SprintServiceImpl(sprintRepository, projetRepository, sprintMapper, accesProjetService, habilitationService);
    }

    private SprintRequest requeteValide() {
        SprintRequest request = new SprintRequest();
        request.setNom("Sprint 1");
        request.setDateDebut(LocalDate.now());
        request.setDateFin(LocalDate.now().plusWeeks(2));
        request.setObjectif("Livrer la premiere iteration");
        return request;
    }

    @Test
    void creer_projetExistant_associeLeSprintAuProjet() {
        Projet projet = new Projet();
        projet.setId(1L);
        SprintRequest request = requeteValide();

        Sprint sprintAEnregistrer = new Sprint();
        Sprint sprintEnregistre = new Sprint();
        sprintEnregistre.setId(10L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(sprintMapper.toEntity(request)).thenReturn(sprintAEnregistrer);
        when(sprintRepository.save(sprintAEnregistrer)).thenReturn(sprintEnregistre);
        when(sprintMapper.toResponse(sprintEnregistre)).thenReturn(new SprintResponse());

        service.creer(1L, request);

        assertThat(sprintAEnregistrer.getProjet()).isEqualTo(projet);
    }

    @Test
    void creer_datesChevauchantUnSprintExistant_leveSprintChevauchementException() {
        Projet projet = new Projet();
        projet.setId(1L);

        Sprint sprintExistant = new Sprint();
        sprintExistant.setId(5L);
        sprintExistant.setDateDebut(LocalDate.now());
        sprintExistant.setDateFin(LocalDate.now().plusWeeks(2));

        SprintRequest request = new SprintRequest();
        request.setNom("Sprint chevauchant");
        request.setDateDebut(LocalDate.now().plusWeeks(1));
        request.setDateFin(LocalDate.now().plusWeeks(3));
        request.setObjectif("Objectif");

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(sprintRepository.findByProjetId(1L)).thenReturn(Collections.singletonList(sprintExistant));

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(SprintChevauchementException.class);
    }

    @Test
    void creer_datesConsecutivesSansChevauchement_creeLeSprint() {
        Projet projet = new Projet();
        projet.setId(1L);

        Sprint sprintExistant = new Sprint();
        sprintExistant.setId(5L);
        sprintExistant.setDateDebut(LocalDate.now());
        sprintExistant.setDateFin(LocalDate.now().plusWeeks(2));

        SprintRequest request = new SprintRequest();
        request.setNom("Sprint suivant");
        request.setDateDebut(LocalDate.now().plusWeeks(2).plusDays(1));
        request.setDateFin(LocalDate.now().plusWeeks(4));
        request.setObjectif("Objectif");

        Sprint sprintAEnregistrer = new Sprint();
        Sprint sprintEnregistre = new Sprint();
        sprintEnregistre.setId(10L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(sprintRepository.findByProjetId(1L)).thenReturn(Collections.singletonList(sprintExistant));
        when(sprintMapper.toEntity(request)).thenReturn(sprintAEnregistrer);
        when(sprintRepository.save(sprintAEnregistrer)).thenReturn(sprintEnregistre);
        when(sprintMapper.toResponse(sprintEnregistre)).thenReturn(new SprintResponse());

        service.creer(1L, request);

        assertThat(sprintAEnregistrer.getProjet()).isEqualTo(projet);
    }

    @Test
    void creer_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(999L, requeteValide()))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void creer_utilisateurNonMembreDuProjet_neCreePasLeSprint() {
        Projet projet = new Projet();
        projet.setId(1L);
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(projet);

        assertThatThrownBy(() -> service.creer(1L, requeteValide()))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void rechercher_appelleLeControleDeLectureSurLeProjet() {
        Projet projet = new Projet();
        projet.setId(1L);
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(sprintRepository.findByProjetId(eq(1L), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.<Sprint>emptyList()));

        service.rechercher(1L, org.springframework.data.domain.PageRequest.of(0, 10));

        verify(accesProjetService).verifierAccesLecture(projet);
    }

    @Test
    void obtenir_sprintAppartenantAUnAutreProjet_leveSprintIntrouvableException() {
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(autreProjet);

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service.obtenir(1L, 10L))
                .isInstanceOf(SprintIntrouvableException.class);
    }

    @Test
    void obtenir_sprintInexistant_leveSprintIntrouvableException() {
        when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(1L, 999L))
                .isInstanceOf(SprintIntrouvableException.class);
    }

    @Test
    void modifier_sprintDuBonProjet_metAJourEtRetourne() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        SprintRequest request = requeteValide();

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(new SprintResponse());

        service.modifier(1L, 10L, request);

        verify(sprintMapper).updateEntity(request, sprint);
    }

    @Test
    void modifier_datesChevauchantUnAutreSprint_leveSprintChevauchementException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);

        Sprint autreSprint = new Sprint();
        autreSprint.setId(20L);
        autreSprint.setDateDebut(LocalDate.now().plusWeeks(4));
        autreSprint.setDateFin(LocalDate.now().plusWeeks(6));

        SprintRequest request = requeteValide();
        request.setDateDebut(LocalDate.now().plusWeeks(5));
        request.setDateFin(LocalDate.now().plusWeeks(7));

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjetId(1L)).thenReturn(Collections.singletonList(autreSprint));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(SprintChevauchementException.class);
    }

    @Test
    void modifier_datesInchangeesSurLeSprintLuiMeme_neLevePasDeException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        sprint.setDateDebut(LocalDate.now());
        sprint.setDateFin(LocalDate.now().plusWeeks(2));

        SprintRequest request = requeteValide();

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjetId(1L)).thenReturn(Collections.singletonList(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(new SprintResponse());

        service.modifier(1L, 10L, request);

        verify(sprintMapper).updateEntity(request, sprint);
    }

    @Test
    void supprimer_sprintDuBonProjet_appelleDelete() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        service.supprimer(1L, 10L);

        verify(sprintRepository).delete(sprint);
    }

    @Test
    void supprimer_sprintAppartenantAUnAutreProjet_leveSprintIntrouvableExceptionSansSupprimer() {
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(autreProjet);

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service.supprimer(1L, 10L))
                .isInstanceOf(SprintIntrouvableException.class);
    }

    @Test
    void commencer_sprintPlanifie_passeEnCours() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        sprint.setStatut(StatutSprint.PLANIFIE);

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(new SprintResponse());

        service.commencer(1L, 10L);

        assertThat(sprint.getStatut()).isEqualTo(StatutSprint.EN_COURS);
    }

    @Test
    void commencer_sprintDejaEnCours_leveStatutSprintInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        sprint.setStatut(StatutSprint.EN_COURS);

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service.commencer(1L, 10L))
                .isInstanceOf(StatutSprintInvalideException.class);
    }

    @Test
    void commencer_avantSaDateMaisSprintPrecedentNonCloture_leveSprintPrecedentNonClotureException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint precedent = sprintDuProjet(projet, 9L, StatutSprint.EN_COURS, LocalDate.now().minusWeeks(2));
        Sprint sprint = sprintDuProjet(projet, 10L, StatutSprint.PLANIFIE, LocalDate.now().plusWeeks(1));

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjetId(1L)).thenReturn(Arrays.asList(precedent, sprint));

        assertThatThrownBy(() -> service.commencer(1L, 10L))
                .isInstanceOf(SprintPrecedentNonClotureException.class);
    }

    @Test
    void commencer_avantSaDateEtSprintPrecedentCloture_passeEnCours() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint precedent = sprintDuProjet(projet, 9L, StatutSprint.CLOTURER, LocalDate.now().minusWeeks(2));
        Sprint sprint = sprintDuProjet(projet, 10L, StatutSprint.PLANIFIE, LocalDate.now().plusWeeks(1));

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.findByProjetId(1L)).thenReturn(Arrays.asList(precedent, sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(new SprintResponse());

        service.commencer(1L, 10L);

        assertThat(sprint.getStatut()).isEqualTo(StatutSprint.EN_COURS);
    }

    @Test
    void demarrerLesSprintsArrivesADateDeDebut_demarreSeulementSiLePrecedentEstCloture() {
        Projet projetA = new Projet();
        projetA.setId(1L);
        Sprint precedentCloture = sprintDuProjet(projetA, 1L, StatutSprint.CLOTURER, LocalDate.now().minusWeeks(2));
        Sprint aDemarrer = sprintDuProjet(projetA, 2L, StatutSprint.PLANIFIE, LocalDate.now());

        Projet projetB = new Projet();
        projetB.setId(2L);
        Sprint precedentEnRetard = sprintDuProjet(projetB, 3L, StatutSprint.EN_COURS, LocalDate.now().minusWeeks(2));
        Sprint enAttente = sprintDuProjet(projetB, 4L, StatutSprint.PLANIFIE, LocalDate.now());

        when(sprintRepository.findByStatutAndDateDebutLessThanEqualOrderByDateDebutAsc(StatutSprint.PLANIFIE, LocalDate.now()))
                .thenReturn(Arrays.asList(aDemarrer, enAttente));
        when(sprintRepository.findByProjetId(1L)).thenReturn(Arrays.asList(precedentCloture, aDemarrer));
        when(sprintRepository.findByProjetId(2L)).thenReturn(Arrays.asList(precedentEnRetard, enAttente));

        service.demarrerLesSprintsArrivesADateDeDebut();

        assertThat(aDemarrer.getStatut()).isEqualTo(StatutSprint.EN_COURS);
        assertThat(enAttente.getStatut()).isEqualTo(StatutSprint.PLANIFIE);
    }

    private Sprint sprintDuProjet(Projet projet, Long id, StatutSprint statut, LocalDate dateDebut) {
        Sprint sprint = new Sprint();
        sprint.setId(id);
        sprint.setNom("Sprint " + id);
        sprint.setProjet(projet);
        sprint.setStatut(statut);
        sprint.setDateDebut(dateDebut);
        sprint.setDateFin(dateDebut.plusDays(13));
        return sprint;
    }

    @Test
    void terminer_sprintEnCours_passeEnCloture() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        sprint.setStatut(StatutSprint.EN_COURS);

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        when(sprintMapper.toResponse(sprint)).thenReturn(new SprintResponse());

        service.terminer(1L, 10L);

        assertThat(sprint.getStatut()).isEqualTo(StatutSprint.CLOTURER);
    }

    @Test
    void terminer_sprintEncorePlanifie_leveStatutSprintInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        sprint.setStatut(StatutSprint.PLANIFIE);

        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service.terminer(1L, 10L))
                .isInstanceOf(StatutSprintInvalideException.class);
    }
}
