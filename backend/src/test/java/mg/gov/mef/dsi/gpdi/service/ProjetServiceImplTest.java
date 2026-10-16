package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.ProjetRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ProjetResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
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
import mg.gov.mef.dsi.gpdi.service.impl.ProjetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjetServiceImplTest {

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private EquipeRepository equipeRepository;

    @Mock
    private ProjetMapper projetMapper;

    @Mock
    private HabilitationService habilitationService;

    @Mock
    private TacheRepository tacheRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private JalonRepository jalonRepository;

    @Mock
    private NotificationService notificationService;

    private ProjetServiceImpl service;

    @BeforeEach
    void initService() {
        service = new ProjetServiceImpl(projetRepository, utilisateurRepository, equipeRepository, projetMapper,
                habilitationService, tacheRepository, sprintRepository, jalonRepository, notificationService);
    }

    @Test
    void obtenir_calculeLaProgressionAPartirDesTachesTerminees() {
        Projet projet = new Projet();
        projet.setId(1L);
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());
        when(tacheRepository.countByProjetId(1L)).thenReturn(3L);
        when(tacheRepository.countByProjetIdAndStatut(1L, StatutTache.TERMINEE)).thenReturn(1L);

        ProjetResponse reponse = service.obtenir(1L);

        assertThat(reponse.getProgression()).isEqualTo(33);
    }

    @Test
    void obtenir_projetSansTache_progressionAZero() {
        Projet projet = new Projet();
        projet.setId(1L);
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());
        when(tacheRepository.countByProjetId(1L)).thenReturn(0L);

        ProjetResponse reponse = service.obtenir(1L);

        assertThat(reponse.getProgression()).isZero();
    }

    private Utilisateur responsableEligible(Long id) {
        Role role = new Role();
        role.setNom("chef_projet");
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setRole(role);
        return utilisateur;
    }

    private ProjetRequest requeteValide() {
        ProjetRequest request = new ProjetRequest();
        request.setTitre("Projet Alpha");
        request.setDateDebut(LocalDate.now());
        request.setDateFin(LocalDate.now().plusMonths(3));
        request.setPriorite(Priorite.NORMALE);
        request.setIdResponsable(1L);
        request.setIdEquipe(10L);
        return request;
    }

    @Test
    void creer_statutParDefautEnPreparation_nactivePasLequipe() {
        ProjetRequest request = requeteValide();
        Utilisateur responsable = responsableEligible(1L);
        Equipe equipe = new Equipe();
        equipe.setId(10L);
        equipe.setActive(false);

        Projet projetAEnregistrer = new Projet();
        Projet projetEnregistre = new Projet();
        projetEnregistre.setId(100L);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsable));
        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(projetMapper.toEntity(request)).thenReturn(projetAEnregistrer);
        when(projetRepository.save(projetAEnregistrer)).thenReturn(projetEnregistre);
        when(projetRepository.save(projetEnregistre)).thenReturn(projetEnregistre);
        when(projetMapper.toResponse(projetEnregistre)).thenReturn(new ProjetResponse());

        service.creer(request);

        assertThat(equipe.getActive()).isFalse();
        verify(equipeRepository, never()).save(equipe);
    }

    @Test
    void creer_statutEnCoursDemandeDirectement_activeLequipe() {
        ProjetRequest request = requeteValide();
        request.setStatut(StatutProjet.EN_COURS);
        Utilisateur responsable = responsableEligible(1L);
        Equipe equipe = new Equipe();
        equipe.setId(10L);
        equipe.setActive(false);

        Projet projetAEnregistrer = new Projet();
        Projet projetEnregistre = new Projet();
        projetEnregistre.setId(100L);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsable));
        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(projetMapper.toEntity(request)).thenReturn(projetAEnregistrer);
        when(projetRepository.save(projetAEnregistrer)).thenReturn(projetEnregistre);
        when(projetRepository.save(projetEnregistre)).thenReturn(projetEnregistre);
        when(projetMapper.toResponse(projetEnregistre)).thenReturn(new ProjetResponse());

        service.creer(request);

        assertThat(equipe.getActive()).isTrue();
        verify(equipeRepository).save(equipe);
    }

    @Test
    void creer_statutEnCoursEtEquipeDejaActive_neResauvegardePasLequipe() {
        ProjetRequest request = requeteValide();
        request.setStatut(StatutProjet.EN_COURS);
        Utilisateur responsable = responsableEligible(1L);
        Equipe equipe = new Equipe();
        equipe.setId(10L);
        equipe.setActive(true);

        Projet projetEnregistre = new Projet();
        projetEnregistre.setId(100L);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsable));
        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(projetMapper.toEntity(request)).thenReturn(new Projet());
        lenient().when(projetRepository.save(org.mockito.ArgumentMatchers.any(Projet.class))).thenReturn(projetEnregistre);
        lenient().when(projetMapper.toResponse(org.mockito.ArgumentMatchers.any(Projet.class))).thenReturn(new ProjetResponse());

        service.creer(request);

        verify(equipeRepository, never()).save(equipe);
    }

    @Test
    void creer_sansEquipe_neLevePasEtNappelleJamaisEquipeRepository() {
        ProjetRequest request = requeteValide();
        request.setIdEquipe(null);
        Utilisateur responsable = responsableEligible(1L);

        Projet projetAEnregistrer = new Projet();
        Projet projetEnregistre = new Projet();
        projetEnregistre.setId(100L);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsable));
        when(projetMapper.toEntity(request)).thenReturn(projetAEnregistrer);
        when(projetRepository.save(projetAEnregistrer)).thenReturn(projetEnregistre);
        when(projetRepository.save(projetEnregistre)).thenReturn(projetEnregistre);
        when(projetMapper.toResponse(projetEnregistre)).thenReturn(new ProjetResponse());

        ProjetResponse resultat = service.creer(request);

        assertThat(resultat).isNotNull();
        assertThat(projetAEnregistrer.getEquipe()).isNull();
        verify(equipeRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void demarrerLesProjetsArrivesADateDeDebut_projetEnPreparation_passeEnCoursEtActiveLequipe() {
        Equipe equipe = new Equipe();
        equipe.setId(10L);
        equipe.setActive(false);
        Projet projet = new Projet();
        projet.setId(100L);
        projet.setStatut(StatutProjet.EN_PREPARATION);
        projet.setEquipe(equipe);

        when(projetRepository.findByStatutAndDateDebutLessThanEqual(StatutProjet.EN_PREPARATION, LocalDate.now()))
                .thenReturn(java.util.Collections.singletonList(projet));
        when(projetRepository.save(projet)).thenReturn(projet);

        service.demarrerLesProjetsArrivesADateDeDebut();

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.EN_COURS);
        assertThat(equipe.getActive()).isTrue();
    }

    @Test
    void demarrer_projetEnPreparation_passeEnCoursEtActiveLequipe() {
        Equipe equipe = new Equipe();
        equipe.setId(10L);
        equipe.setActive(false);
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setStatut(StatutProjet.EN_PREPARATION);
        projet.setEquipe(equipe);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.demarrer(1L);

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.EN_COURS);
        assertThat(equipe.getActive()).isTrue();
    }

    @Test
    void demarrer_projetDejaEnCours_leveStatutProjetInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setStatut(StatutProjet.EN_COURS);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThatThrownBy(() -> service.demarrer(1L))
                .isInstanceOf(StatutProjetInvalideException.class);

        verify(projetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void demarrer_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.demarrer(999L))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void reprendre_projetSuspendu_passeEnCoursEtActiveLequipe() {
        Equipe equipe = new Equipe();
        equipe.setId(10L);
        equipe.setActive(false);
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setStatut(StatutProjet.SUSPENDU);
        projet.setEquipe(equipe);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.reprendre(1L);

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.EN_COURS);
        assertThat(equipe.getActive()).isTrue();
    }

    @Test
    void reprendre_projetNonSuspendu_leveStatutProjetInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setStatut(StatutProjet.EN_COURS);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThatThrownBy(() -> service.reprendre(1L))
                .isInstanceOf(StatutProjetInvalideException.class);

        verify(projetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reprendre_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reprendre(999L))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void suspendre_projetExistant_fixeStatutSuspendu() {
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setStatut(StatutProjet.EN_COURS);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.suspendre(1L);

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.SUSPENDU);
    }

    @Test
    void suspendre_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspendre(999L))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void modifier_statutNonFourni_conserveLeStatutCourant() {
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setStatut(StatutProjet.EN_COURS);
        Utilisateur responsable = responsableEligible(1L);
        Equipe equipe = new Equipe();
        equipe.setId(10L);
        equipe.setActive(true);

        ProjetRequest request = requeteValide();
        request.setStatut(null);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsable));
        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.modifier(1L, request);

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.EN_COURS);
    }

    @Test
    void modifier_idEquipeNonFourni_conserveLequipeCourante() {
        Equipe equipeCourante = new Equipe();
        equipeCourante.setId(5L);
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setStatut(StatutProjet.EN_COURS);
        projet.setEquipe(equipeCourante);
        Utilisateur responsable = responsableEligible(1L);

        ProjetRequest request = requeteValide();
        request.setIdEquipe(null);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsable));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.modifier(1L, request);

        assertThat(projet.getEquipe()).isEqualTo(equipeCourante);
        verify(equipeRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void creer_responsableSansRoleChefDeProjet_leveUtilisateurNonEligibleException() {
        ProjetRequest request = requeteValide();
        Role role = new Role();
        role.setNom("membre_equipe");
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setRole(role);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(UtilisateurNonEligibleException.class);
    }

    @Test
    void creer_responsableInexistant_leveUtilisateurIntrouvableException() {
        ProjetRequest request = requeteValide();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }

    @Test
    void creer_equipeInexistante_leveEquipeIntrouvableException() {
        ProjetRequest request = requeteValide();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsableEligible(1L)));
        when(equipeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(EquipeIntrouvableException.class);
    }

    @Test
    void creer_statutClotureDemandeDirectement_leveStatutProjetInvalideException() {
        ProjetRequest request = requeteValide();
        request.setStatut(StatutProjet.CLOTURE);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(responsableEligible(1L)));
        when(equipeRepository.findById(10L)).thenReturn(Optional.of(new Equipe()));
        when(projetMapper.toEntity(request)).thenReturn(new Projet());

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(StatutProjetInvalideException.class);
    }

    @Test
    void obtenir_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(999L))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void cloturer_projetExistant_fixeStatutEtDateCloture() {
        Projet projet = new Projet();
        projet.setId(1L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.cloturer(1L);

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.CLOTURE);
        assertThat(projet.getDateCloture()).isEqualTo(LocalDate.now());
    }

    @Test
    void cloturer_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cloturer(999L))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    private Sprint sprint(StatutSprint statut) {
        Sprint sprint = new Sprint();
        sprint.setStatut(statut);
        return sprint;
    }

    private Jalon jalon(StatutJalon statut) {
        Jalon jalon = new Jalon();
        jalon.setStatut(statut);
        return jalon;
    }

    @Test
    void cloturer_sprintEncoreEnCours_leveClotureProjetImpossibleExceptionEtNeSauvegardeRien() {
        Projet projet = new Projet();
        projet.setId(1L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(sprintRepository.findByProjetId(1L)).thenReturn(java.util.Arrays.asList(
                sprint(StatutSprint.CLOTURER), sprint(StatutSprint.EN_COURS)));

        assertThatThrownBy(() -> service.cloturer(1L))
                .isInstanceOf(ClotureProjetImpossibleException.class);

        verify(projetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cloturer_jalonEncoreAVenir_leveClotureProjetImpossibleException() {
        Projet projet = new Projet();
        projet.setId(1L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(jalonRepository.findByProjetId(1L)).thenReturn(java.util.Arrays.asList(
                jalon(StatutJalon.ATTEINT), jalon(StatutJalon.A_VENIR)));

        assertThatThrownBy(() -> service.cloturer(1L))
                .isInstanceOf(ClotureProjetImpossibleException.class);

        verify(projetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cloturer_jalonEnRetard_leveClotureProjetImpossibleException() {
        Projet projet = new Projet();
        projet.setId(1L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(jalonRepository.findByProjetId(1L)).thenReturn(java.util.Collections.singletonList(jalon(StatutJalon.EN_RETARD)));

        assertThatThrownBy(() -> service.cloturer(1L))
                .isInstanceOf(ClotureProjetImpossibleException.class);
    }

    @Test
    void cloturer_tousSprintsClotureesEtJalonsAtteintsOuAnnules_cloture() {
        Projet projet = new Projet();
        projet.setId(1L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(sprintRepository.findByProjetId(1L)).thenReturn(java.util.Collections.singletonList(sprint(StatutSprint.CLOTURER)));
        when(jalonRepository.findByProjetId(1L)).thenReturn(java.util.Arrays.asList(
                jalon(StatutJalon.ATTEINT), jalon(StatutJalon.ANNULER)));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.cloturer(1L);

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.CLOTURE);
    }

    @Test
    void archiver_projetExistant_fixeStatutEtArchiveTrue() {
        Projet projet = new Projet();
        projet.setId(1L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(projetRepository.save(projet)).thenReturn(projet);
        when(projetMapper.toResponse(projet)).thenReturn(new ProjetResponse());

        service.archiver(1L);

        assertThat(projet.getStatut()).isEqualTo(StatutProjet.ARCHIVE);
        assertThat(projet.getArchive()).isTrue();
    }

    @Test
    void archiver_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiver(999L))
                .isInstanceOf(ProjetIntrouvableException.class);
    }
}
