package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.LivrableRejetRequest;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRequest;
import mg.gov.mef.dsi.gpdi.dto.response.LivrableResponse;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Livrable;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;
import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;
import mg.gov.mef.dsi.gpdi.exception.DateLivraisonInvalideException;
import mg.gov.mef.dsi.gpdi.exception.JalonIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.LivrableIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutJalonInvalideException;
import mg.gov.mef.dsi.gpdi.exception.StatutLivrableInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.LivrableMapper;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.LivrableRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.impl.LivrableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivrableServiceImplTest {

    @Mock
    private LivrableRepository livrableRepository;

    @Mock
    private JalonRepository jalonRepository;

    @Mock
    private LivrableMapper livrableMapper;

    @Mock
    private AccesProjetService accesProjetService;

    @Mock
    private HabilitationService habilitationService;

    private LivrableServiceImpl service;

    @BeforeEach
    void initService() {
        service = new LivrableServiceImpl(livrableRepository, jalonRepository, livrableMapper, accesProjetService, habilitationService);
    }

    private Jalon jalonAvecEcheance(Long id, LocalDate dateEcheance) {
        Jalon jalon = new Jalon();
        jalon.setId(id);
        jalon.setDateEcheance(dateEcheance);
        return jalon;
    }

    @Test
    void creer_dateLivraisonAvantEcheanceJalon_enregistreLeLivrable() {
        Jalon jalon = jalonAvecEcheance(1L, LocalDate.now().plusWeeks(2));
        LivrableRequest request = new LivrableRequest();
        request.setDescription("Documentation");
        request.setType("Document");
        request.setDateLivraison(LocalDate.now().plusWeeks(1));

        Livrable livrableAEnregistrer = new Livrable();
        Livrable livrableEnregistre = new Livrable();
        livrableEnregistre.setId(10L);

        when(jalonRepository.findById(1L)).thenReturn(Optional.of(jalon));
        when(livrableMapper.toEntity(request)).thenReturn(livrableAEnregistrer);
        when(livrableRepository.save(livrableAEnregistrer)).thenReturn(livrableEnregistre);
        when(livrableMapper.toResponse(livrableEnregistre)).thenReturn(new LivrableResponse());

        service.creer(1L, request);

        assertThat(livrableAEnregistrer.getJalon()).isEqualTo(jalon);
    }

    @Test
    void creer_dateLivraisonApresEcheanceJalon_leveDateLivraisonInvalideException() {
        Jalon jalon = jalonAvecEcheance(1L, LocalDate.now());
        LivrableRequest request = new LivrableRequest();
        request.setDescription("Documentation");
        request.setType("Document");
        request.setDateLivraison(LocalDate.now().plusDays(1));

        when(jalonRepository.findById(1L)).thenReturn(Optional.of(jalon));

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(DateLivraisonInvalideException.class);
    }

    @Test
    void creer_jalonAtteint_leveStatutJalonInvalideException() {
        Jalon jalon = jalonAvecEcheance(1L, LocalDate.now().plusWeeks(2));
        jalon.setStatut(StatutJalon.ATTEINT);
        LivrableRequest request = new LivrableRequest();
        request.setDescription("Documentation");
        request.setType("Document");
        request.setDateLivraison(LocalDate.now().plusWeeks(1));

        when(jalonRepository.findById(1L)).thenReturn(Optional.of(jalon));

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(StatutJalonInvalideException.class);
    }

    @Test
    void creer_jalonInexistant_leveJalonIntrouvableException() {
        when(jalonRepository.findById(999L)).thenReturn(Optional.empty());

        LivrableRequest request = new LivrableRequest();
        request.setDateLivraison(LocalDate.now());

        assertThatThrownBy(() -> service.creer(999L, request))
                .isInstanceOf(JalonIntrouvableException.class);
    }

    @Test
    void creer_utilisateurNonMembreDuProjet_neCreePasLeLivrable() {
        Jalon jalon = jalonAvecEcheance(1L, LocalDate.now().plusWeeks(2));
        when(jalonRepository.findById(1L)).thenReturn(Optional.of(jalon));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(jalon.getProjet());

        LivrableRequest request = new LivrableRequest();
        request.setDateLivraison(LocalDate.now());

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(livrableRepository, never()).save(any());
    }

    @Test
    void obtenir_appelleLeControleDeLecturePartageableSurLeProjetDuJalon() {
        Jalon jalon = new Jalon();
        jalon.setId(1L);
        Livrable livrable = new Livrable();
        livrable.setId(10L);
        livrable.setJalon(jalon);

        when(livrableRepository.findById(10L)).thenReturn(Optional.of(livrable));
        when(livrableMapper.toResponse(livrable)).thenReturn(new LivrableResponse());

        service.obtenir(1L, 10L);

        verify(accesProjetService).verifierAccesLecturePartageable(jalon.getProjet());
    }

    @Test
    void obtenir_livrableAppartenantAUnAutreJalon_leveLivrableIntrouvableException() {
        Jalon autreJalon = new Jalon();
        autreJalon.setId(2L);
        Livrable livrable = new Livrable();
        livrable.setId(10L);
        livrable.setJalon(autreJalon);

        when(livrableRepository.findById(10L)).thenReturn(Optional.of(livrable));

        assertThatThrownBy(() -> service.obtenir(1L, 10L))
                .isInstanceOf(LivrableIntrouvableException.class);
    }

    @Test
    void livrer_livrableAttendu_passeEnLivreEtFixeDateValidation() {
        Jalon jalon = new Jalon();
        jalon.setId(1L);
        Livrable livrable = new Livrable();
        livrable.setId(10L);
        livrable.setJalon(jalon);
        livrable.setStatut(StatutLivrable.ATTENDU);

        when(livrableRepository.findById(10L)).thenReturn(Optional.of(livrable));
        when(livrableRepository.save(livrable)).thenReturn(livrable);
        when(livrableMapper.toResponse(livrable)).thenReturn(new LivrableResponse());

        service.livrer(1L, 10L);

        assertThat(livrable.getStatut()).isEqualTo(StatutLivrable.LIVRE);
        assertThat(livrable.getDateValidation()).isEqualTo(LocalDate.now());
    }

    @Test
    void livrer_livrableDejaLivre_leveStatutLivrableInvalideException() {
        Jalon jalon = new Jalon();
        jalon.setId(1L);
        Livrable livrable = new Livrable();
        livrable.setId(10L);
        livrable.setJalon(jalon);
        livrable.setStatut(StatutLivrable.LIVRE);

        when(livrableRepository.findById(10L)).thenReturn(Optional.of(livrable));

        assertThatThrownBy(() -> service.livrer(1L, 10L))
                .isInstanceOf(StatutLivrableInvalideException.class);
    }

    @Test
    void rejeter_livrableAttendu_passeEnRejeteAvecMotif() {
        Jalon jalon = new Jalon();
        jalon.setId(1L);
        Livrable livrable = new Livrable();
        livrable.setId(10L);
        livrable.setJalon(jalon);
        livrable.setStatut(StatutLivrable.ATTENDU);

        LivrableRejetRequest request = new LivrableRejetRequest();
        request.setMotifRejet("Non conforme");

        when(livrableRepository.findById(10L)).thenReturn(Optional.of(livrable));
        when(livrableRepository.save(livrable)).thenReturn(livrable);
        when(livrableMapper.toResponse(livrable)).thenReturn(new LivrableResponse());

        service.rejeter(1L, 10L, request);

        assertThat(livrable.getStatut()).isEqualTo(StatutLivrable.REJETE);
        assertThat(livrable.getMotifRejet()).isEqualTo("Non conforme");
        assertThat(livrable.getDateRejet()).isEqualTo(LocalDate.now());
    }

    @Test
    void rejeter_livrableDejaRejete_leveStatutLivrableInvalideException() {
        Jalon jalon = new Jalon();
        jalon.setId(1L);
        Livrable livrable = new Livrable();
        livrable.setId(10L);
        livrable.setJalon(jalon);
        livrable.setStatut(StatutLivrable.REJETE);

        LivrableRejetRequest request = new LivrableRejetRequest();
        request.setMotifRejet("Non conforme");

        when(livrableRepository.findById(10L)).thenReturn(Optional.of(livrable));

        assertThatThrownBy(() -> service.rejeter(1L, 10L, request))
                .isInstanceOf(StatutLivrableInvalideException.class);
    }
}
