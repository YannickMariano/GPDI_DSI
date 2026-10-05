package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurAssigneResponse;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TachePrecedence;
import mg.gov.mef.dsi.gpdi.entity.TypeLien;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.DateFinTacheInvalideException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SprintProjetIncoherentException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceInvalideException;
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
import mg.gov.mef.dsi.gpdi.service.impl.TacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TacheServiceImplTest {

    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private MembreEquipeRepository membreEquipeRepository;
    @Mock
    private AffectationTacheRepository affectationTacheRepository;
    @Mock
    private SousTacheRepository sousTacheRepository;
    @Mock
    private CommentaireRepository commentaireRepository;
    @Mock
    private TachePrecedenceRepository tachePrecedenceRepository;
    @Mock
    private TacheMapper tacheMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AccesProjetService accesProjetService;
    @Mock
    private HabilitationService habilitationService;

    private TacheServiceImpl service;

    @BeforeEach
    void initService() {
        service = new TacheServiceImpl(tacheRepository, projetRepository, sprintRepository, utilisateurRepository,
                membreEquipeRepository, affectationTacheRepository, sousTacheRepository, commentaireRepository,
                tachePrecedenceRepository, tacheMapper, notificationService, accesProjetService, habilitationService);
    }

    private TacheRequest requeteValide() {
        TacheRequest request = new TacheRequest();
        request.setTitre("Developper API");
        request.setPriorite(Priorite.HAUTE);
        request.setDateEcheance(LocalDate.now().plusWeeks(2));
        request.setIdsUtilisateursAssignes(Collections.singletonList(5L));
        return request;
    }

    @Test
    void creer_membreEligible_genereLeCodeApresLePremierEnregistrement() {
        Equipe equipe = new Equipe();
        equipe.setId(20L);
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setEquipe(equipe);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(5L);

        TacheRequest request = requeteValide();
        Tache tacheAEnregistrer = new Tache();

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(utilisateur));
        when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(20L, 5L)).thenReturn(true);
        when(tacheMapper.toEntity(request)).thenAnswer(inv -> tacheAEnregistrer);

        when(tacheRepository.save(any(Tache.class))).thenAnswer(invocation -> {
            Tache t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(42L);
            }
            return t;
        });
        AffectationTache affectation = new AffectationTache();
        when(affectationTacheRepository.save(any(AffectationTache.class))).thenReturn(affectation);
        when(tacheMapper.toAssignationResponse(affectation))
                .thenReturn(new UtilisateurAssigneResponse(1L, 5L, "M1", "Nom", "Prenom", null));
        when(tacheMapper.toResponse(tacheAEnregistrer)).thenReturn(new TacheResponse());

        service.creer(1L, request);

        assertThat(tacheAEnregistrer.getCode()).isEqualTo("T-42");
        assertThat(tacheAEnregistrer.getProjet()).isEqualTo(projet);
    }

    @Test
    void creer_utilisateurNonMembreDeLequipe_leveUtilisateurNonMembreEquipeException() {
        Equipe equipe = new Equipe();
        equipe.setId(20L);
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setEquipe(equipe);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(5L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(utilisateur));
        when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(20L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> service.creer(1L, requeteValide()))
                .isInstanceOf(UtilisateurNonMembreEquipeException.class);
    }

    @Test
    void creer_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(999L, requeteValide()))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void creer_utilisateurNonMembreDuProjet_neCreePasLaTache() {
        Projet projet = new Projet();
        projet.setId(1L);
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(projet);

        assertThatThrownBy(() -> service.creer(1L, requeteValide()))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(tacheRepository, never()).save(any());
    }

    @Test
    void obtenir_appelleLeControleDeLectureSurLeProjetDeLaTache() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheMapper.toDetailResponse(tache)).thenReturn(new mg.gov.mef.dsi.gpdi.dto.response.TacheDetailResponse());

        service.obtenir(1L, 10L);

        verify(accesProjetService).verifierAccesLecture(projet);
    }

    @Test
    void modifier_utilisateurNonMembreDuProjet_neModifiePasLaTache() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(projet);

        assertThatThrownBy(() -> service.modifier(1L, 10L, requeteValide()))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(tacheRepository, never()).save(any());
    }

    @Test
    void creer_sprintDunAutreProjet_leveSprintProjetIncoherentException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        Sprint sprint = new Sprint();
        sprint.setId(30L);
        sprint.setProjet(autreProjet);

        TacheRequest request = requeteValide();
        request.setIdSprint(30L);
        request.setIdsUtilisateursAssignes(Collections.emptyList());

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(sprintRepository.findById(30L)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(SprintProjetIncoherentException.class);
    }

    @Test
    void obtenir_tacheAppartenantAUnAutreProjet_leveTacheIntrouvableException() {
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(autreProjet);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));

        assertThatThrownBy(() -> service.obtenir(1L, 10L))
                .isInstanceOf(TacheIntrouvableException.class);
    }

    @Test
    void supprimer_tacheExistante_supprimeLesDonneesLieesPuisLaTache() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));

        service.supprimer(1L, 10L);

        verify(affectationTacheRepository).deleteByTacheId(10L);
        verify(sousTacheRepository).deleteByTacheId(10L);
        verify(commentaireRepository).deleteByTacheId(10L);
        verify(tachePrecedenceRepository).deleteByTacheSourceIdOrTacheCibleId(10L, 10L);
        verify(tacheRepository).delete(tache);
    }

    @Test
    void modifier_statutNonFourni_conserveLeStatutCourant() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.EN_COURS);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getStatut()).isEqualTo(StatutTache.EN_COURS);
    }

    @Test
    void modifier_statutFourni_metAJourLeStatut() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_COURS);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getStatut()).isEqualTo(StatutTache.EN_COURS);
    }

    @Test
    void modifier_passageEnCoursDepuisAFaire_demarreLeChronometre() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_COURS);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getDemarreeLe()).isNotNull();
    }

    @Test
    void modifier_passageEnCoursAlorsQueDejaDemarree_neReinitialisePasLeChronometre() {
        Projet projet = new Projet();
        projet.setId(1L);
        LocalDateTime demarrageInitial = LocalDateTime.now().minusHours(2);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.BLOQUEE);
        tache.setDemarreeLe(demarrageInitial);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_COURS);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getDemarreeLe()).isEqualTo(demarrageInitial);
    }

    @Test
    void modifier_passageTermineeAvecChronometreActif_figeLeTempsPasseEtArreteLeChronometre() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.EN_COURS);
        tache.setDemarreeLe(LocalDateTime.now().minusHours(2));
        tache.setTempsPasse(3.0);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.TERMINEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getDemarreeLe()).isNull();
        assertThat(tache.getDateFinReelle()).isEqualTo(LocalDate.now());
        assertThat(tache.getTempsPasse()).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void modifier_passageTermineeSansChronometreActif_neModifiePasLeTempsPasse() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);
        tache.setTempsPasse(3.0);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.TERMINEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getTempsPasse()).isEqualTo(3.0);
    }

    @Test
    void modifier_passageTermineeSprintDejaCloture_leveDateFinTacheInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(5L);
        sprint.setProjet(projet);
        sprint.setDateFin(LocalDate.now().minusDays(1));
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setIdSprint(5L);
        request.setStatut(StatutTache.TERMINEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(DateFinTacheInvalideException.class);
    }

    @Test
    void modifier_passageTermineeSprintEncoreEnCours_neLevePasDException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(5L);
        sprint.setProjet(projet);
        sprint.setDateFin(LocalDate.now().plusDays(1));
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setIdSprint(5L);
        request.setStatut(StatutTache.TERMINEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getStatut()).isEqualTo(StatutTache.TERMINEE);
    }

    @Test
    void modifier_tempsPasseFourni_ecraseLeTempsPasseCourant() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setTempsPasse(3.0);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setTempsPasse(7.5);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getTempsPasse()).isEqualTo(7.5);
    }

    @Test
    void modifier_tempsPasseNonFourni_conserveLeTempsPasseCourant() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setTempsPasse(3.0);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getTempsPasse()).isEqualTo(3.0);
    }

    @Test
    void modifier_demarrageAvecSourceFinDebutNonTerminee_leveTachePrecedenceInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.EN_COURS);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.FIN_DEBUT);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_COURS);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
    }

    @Test
    void modifier_demarrageAvecSourceFinDebutTerminee_demarreLaTache() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.TERMINEE);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.FIN_DEBUT);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_COURS);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getStatut()).isEqualTo(StatutTache.EN_COURS);
        assertThat(tache.getDemarreeLe()).isNotNull();
    }

    @Test
    void modifier_demarrageAvecSourceDebutDebutJamaisDemarree_leveTachePrecedenceInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.A_FAIRE);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.DEBUT_DEBUT);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_COURS);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
    }

    @Test
    void modifier_demarrageAvecSourceDebutDebutDejaDemarree_demarreLaTache() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.EN_COURS);
        source.setDemarreeLe(LocalDateTime.now().minusHours(1));

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.DEBUT_DEBUT);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_COURS);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getStatut()).isEqualTo(StatutTache.EN_COURS);
    }

    @Test
    void modifier_clotureAvecSourceFinFinNonTerminee_leveTachePrecedenceInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.EN_COURS);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.EN_COURS);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.FIN_FIN);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.TERMINEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
    }

    @Test
    void modifier_clotureAvecSourceDebutFinJamaisDemarree_leveTachePrecedenceInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.A_FAIRE);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.EN_COURS);
        tache.setDemarreeLe(LocalDateTime.now().minusHours(1));

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.DEBUT_FIN);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.TERMINEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
    }

    @Test
    void creer_avecStatutEnCours_neVerifiePasLesDependances() {
        Projet projet = new Projet();
        projet.setId(1L);
        Equipe equipe = new Equipe();
        equipe.setId(20L);
        projet.setEquipe(equipe);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(5L);

        TacheRequest request = requeteValide();
        request.setStatut(StatutTache.EN_COURS);
        Tache tacheAEnregistrer = new Tache();

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(utilisateur));
        when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(20L, 5L)).thenReturn(true);
        when(tacheMapper.toEntity(request)).thenAnswer(inv -> tacheAEnregistrer);
        when(tacheRepository.save(any(Tache.class))).thenAnswer(invocation -> {
            Tache t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(42L);
            }
            return t;
        });
        AffectationTache affectation = new AffectationTache();
        when(affectationTacheRepository.save(any(AffectationTache.class))).thenReturn(affectation);
        when(tacheMapper.toAssignationResponse(affectation))
                .thenReturn(new UtilisateurAssigneResponse(1L, 5L, "M1", "Nom", "Prenom", null));
        when(tacheMapper.toResponse(tacheAEnregistrer)).thenReturn(new TacheResponse());

        service.creer(1L, request);

        assertThat(tacheAEnregistrer.getStatut()).isEqualTo(StatutTache.EN_COURS);
        assertThat(tacheAEnregistrer.getDemarreeLe()).isNotNull();
        verify(tachePrecedenceRepository, never()).findByTacheCibleId(any());
    }

    @Test
    void modifier_depuisAFaireVersEnTestAvecDependanceDemarrageNonSatisfaite_leveTachePrecedenceInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.EN_COURS);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.FIN_DEBUT);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_TEST);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
        assertThat(tache.getStatut()).isEqualTo(StatutTache.A_FAIRE);
    }

    @Test
    void modifier_depuisAFaireVersBloqueeAvecDependanceDemarrageNonSatisfaite_leveTachePrecedenceInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.A_FAIRE);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.DEBUT_DEBUT);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.BLOQUEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
    }

    @Test
    void modifier_depuisAFaireVersTermineeDirectementAvecDependanceDemarrageNonSatisfaite_leveTachePrecedenceInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache source = new Tache();
        source.setId(20L);
        source.setCode("T-20");
        source.setStatut(StatutTache.EN_COURS);

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.A_FAIRE);

        TachePrecedence precedence = new TachePrecedence();
        precedence.setTacheSource(source);
        precedence.setTacheCible(tache);
        precedence.setTypeLien(TypeLien.FIN_DEBUT);

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.TERMINEE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tachePrecedenceRepository.findByTacheCibleId(10L)).thenReturn(Collections.singletonList(precedence));

        assertThatThrownBy(() -> service.modifier(1L, 10L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
    }

    @Test
    void modifier_dejaDemarreeVersEnTest_neRevalidePasLaConditionDeDemarrage() {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(StatutTache.EN_COURS);
        tache.setDemarreeLe(LocalDateTime.now().minusHours(1));

        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_TEST);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());

        service.modifier(1L, 10L, request);

        assertThat(tache.getStatut()).isEqualTo(StatutTache.EN_TEST);
        verify(tachePrecedenceRepository, never()).findByTacheCibleId(any());
    }

    private Tache tacheExistante(StatutTache statut) {
        Projet projet = new Projet();
        projet.setId(1L);
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setProjet(projet);
        tache.setStatut(statut);
        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));
        when(tacheRepository.save(tache)).thenReturn(tache);
        when(tacheMapper.toResponse(tache)).thenReturn(new TacheResponse());
        return tache;
    }

    @Test
    void modifier_passageABloquee_notifieLeBlocage() {
        Tache tache = tacheExistante(StatutTache.EN_COURS);
        tache.setDemarreeLe(LocalDateTime.now().minusHours(1));
        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.BLOQUEE);

        service.modifier(1L, 10L, request);

        verify(notificationService).notifierBlocage(tache, Collections.emptyList());
    }

    @Test
    void modifier_dejaBloquee_neNotifiePasUneSecondeFois() {
        tacheExistante(StatutTache.BLOQUEE);
        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.BLOQUEE);

        service.modifier(1L, 10L, request);

        verify(notificationService, never()).notifierBlocage(any(), any());
    }

    @Test
    void modifier_passageAutreQueBloquee_neNotifiePasDeBlocage() {
        tacheExistante(StatutTache.EN_COURS);
        TacheRequest request = requeteValide();
        request.setIdsUtilisateursAssignes(Collections.emptyList());
        request.setStatut(StatutTache.EN_TEST);

        service.modifier(1L, 10L, request);

        verify(notificationService, never()).notifierBlocage(any(), any());
    }
}
