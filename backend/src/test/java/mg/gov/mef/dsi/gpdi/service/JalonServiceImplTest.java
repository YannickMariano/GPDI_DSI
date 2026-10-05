package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.JalonRequest;
import mg.gov.mef.dsi.gpdi.dto.response.JalonResponse;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;
import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;
import mg.gov.mef.dsi.gpdi.exception.JalonIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.LivrableEnAttenteException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutJalonInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.JalonMapper;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.LivrableRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.impl.JalonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JalonServiceImplTest {

    @Mock
    private JalonRepository jalonRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private JalonMapper jalonMapper;

    @Mock
    private LivrableRepository livrableRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccesProjetService accesProjetService;

    @Mock
    private HabilitationService habilitationService;

    private JalonServiceImpl service;

    @BeforeEach
    void initService() {
        service = new JalonServiceImpl(jalonRepository, projetRepository, jalonMapper, livrableRepository, notificationService, accesProjetService, habilitationService);
    }

    private JalonRequest requeteValide() {
        JalonRequest request = new JalonRequest();
        request.setTitre("Jalon 1");
        request.setDateEcheance(LocalDate.now().plusWeeks(4));
        return request;
    }

    @Test
    void creer_projetExistant_associeLeJalonAuProjet() {
        Projet projet = new Projet();
        projet.setId(1L);
        JalonRequest request = requeteValide();
        Jalon jalonAEnregistrer = new Jalon();
        Jalon jalonEnregistre = new Jalon();
        jalonEnregistre.setId(10L);

        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(jalonMapper.toEntity(request)).thenReturn(jalonAEnregistrer);
        when(jalonRepository.save(jalonAEnregistrer)).thenReturn(jalonEnregistre);
        when(jalonMapper.toResponse(jalonEnregistre)).thenReturn(new JalonResponse());

        service.creer(1L, request);

        assertThat(jalonAEnregistrer.getProjet()).isEqualTo(projet);
    }

    @Test
    void creer_projetInexistant_leveProjetIntrouvableException() {
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(999L, requeteValide()))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void creer_utilisateurNonMembreDuProjet_neCreePasLeJalon() {
        Projet projet = new Projet();
        projet.setId(1L);
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(projet);

        assertThatThrownBy(() -> service.creer(1L, requeteValide()))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(jalonRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void obtenir_appelleLeControleDeLectureSurLeProjetDuJalon() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.ATTEINT);
        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));
        when(jalonMapper.toResponse(jalon)).thenReturn(new JalonResponse());

        service.obtenir(1L, 10L);

        verify(accesProjetService).verifierAccesLecturePartageable(projet);
    }

    @Test
    void obtenir_jalonAppartenantAUnAutreProjet_leveJalonIntrouvableException() {
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(autreProjet);

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));

        assertThatThrownBy(() -> service.obtenir(1L, 10L))
                .isInstanceOf(JalonIntrouvableException.class);
    }

    @Test
    void terminer_jalonAVenir_passeEnAtteintEtFixeDateValidation() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.A_VENIR);

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));
        when(jalonRepository.save(jalon)).thenReturn(jalon);
        when(jalonMapper.toResponse(jalon)).thenReturn(new JalonResponse());

        service.terminer(1L, 10L);

        assertThat(jalon.getStatut()).isEqualTo(StatutJalon.ATTEINT);
        assertThat(jalon.getDateValidation()).isEqualTo(LocalDate.now());
        verify(notificationService).notifierJalonAtteint(jalon);
    }

    @Test
    void modifier_jalonAtteint_leveStatutJalonInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.ATTEINT);

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));

        assertThatThrownBy(() -> service.modifier(1L, 10L, requeteValide()))
                .isInstanceOf(StatutJalonInvalideException.class);
    }

    @Test
    void terminer_jalonAvecLivrableEnAttente_leveLivrableEnAttenteException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.A_VENIR);

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));
        when(livrableRepository.existsByJalonIdAndStatut(10L, StatutLivrable.ATTENDU)).thenReturn(true);

        assertThatThrownBy(() -> service.terminer(1L, 10L))
                .isInstanceOf(LivrableEnAttenteException.class);
    }

    @Test
    void terminer_jalonDejaAtteint_leveStatutJalonInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.ATTEINT);

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));

        assertThatThrownBy(() -> service.terminer(1L, 10L))
                .isInstanceOf(StatutJalonInvalideException.class);
    }

    @Test
    void annuler_jalonEnRetard_passeEnAnnule() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.EN_RETARD);

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));
        when(jalonRepository.save(jalon)).thenReturn(jalon);
        when(jalonMapper.toResponse(jalon)).thenReturn(new JalonResponse());

        service.annuler(1L, 10L);

        assertThat(jalon.getStatut()).isEqualTo(StatutJalon.ANNULER);
    }

    @Test
    void annuler_jalonDejaAnnule_leveStatutJalonInvalideException() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.ANNULER);

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));

        assertThatThrownBy(() -> service.annuler(1L, 10L))
                .isInstanceOf(StatutJalonInvalideException.class);
    }

    @Test
    void obtenir_jalonAVenirEcheanceDepassee_passeEnRetardImmediatementSansAttendreLePlanificateur() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.A_VENIR);
        jalon.setDateEcheance(LocalDate.now().minusDays(1));

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));
        when(jalonRepository.save(jalon)).thenReturn(jalon);
        when(jalonMapper.toResponse(jalon)).thenReturn(new JalonResponse());

        service.obtenir(1L, 10L);

        assertThat(jalon.getStatut()).isEqualTo(StatutJalon.EN_RETARD);
        verify(notificationService).notifierJalonEnRetard(jalon);
    }

    @Test
    void obtenir_jalonAVenirEcheanceFuture_resteAVenir() {
        Projet projet = new Projet();
        projet.setId(1L);
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setProjet(projet);
        jalon.setStatut(StatutJalon.A_VENIR);
        jalon.setDateEcheance(LocalDate.now().plusDays(1));

        when(jalonRepository.findById(10L)).thenReturn(Optional.of(jalon));
        when(jalonMapper.toResponse(jalon)).thenReturn(new JalonResponse());

        service.obtenir(1L, 10L);

        assertThat(jalon.getStatut()).isEqualTo(StatutJalon.A_VENIR);
        verify(jalonRepository, org.mockito.Mockito.never()).save(jalon);
    }

    @Test
    void marquerEnRetardLesJalonsEnRetard_jalonAVenirEcheanceDepassee_passeEnRetard() {
        Jalon jalon = new Jalon();
        jalon.setId(10L);
        jalon.setStatut(StatutJalon.A_VENIR);
        jalon.setDateEcheance(LocalDate.now().minusDays(1));

        when(jalonRepository.findByStatutAndDateEcheanceLessThan(StatutJalon.A_VENIR, LocalDate.now()))
                .thenReturn(Collections.singletonList(jalon));
        when(jalonRepository.save(jalon)).thenReturn(jalon);

        service.marquerEnRetardLesJalonsEnRetard();

        assertThat(jalon.getStatut()).isEqualTo(StatutJalon.EN_RETARD);
        verify(notificationService).notifierJalonEnRetard(jalon);
    }
}
