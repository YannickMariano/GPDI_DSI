package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.DependanceRequest;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TachePrecedenceId;
import mg.gov.mef.dsi.gpdi.entity.TypeLien;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceDejaExistanteException;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.TachePrecedenceInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.TacheMapper;
import mg.gov.mef.dsi.gpdi.repository.TachePrecedenceRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.impl.DependanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependanceServiceImplTest {

    @Mock
    private TachePrecedenceRepository tachePrecedenceRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private TacheMapper tacheMapper;
    @Mock
    private AccesProjetService accesProjetService;

    private DependanceServiceImpl service;

    @BeforeEach
    void initService() {
        service = new DependanceServiceImpl(tachePrecedenceRepository, tacheRepository, tacheMapper, accesProjetService);
    }

    @Test
    void creer_tacheIdentiqueALaCible_leveTachePrecedenceInvalideException() {
        DependanceRequest request = new DependanceRequest();
        request.setIdTacheCible(1L);
        request.setTypeLien(TypeLien.FIN_DEBUT);

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(TachePrecedenceInvalideException.class);
    }

    @Test
    void creer_relationDejaExistanteDansLesDeuxSens_leveTachePrecedenceDejaExistanteException() {
        Tache source = new Tache();
        source.setId(1L);
        Tache cible = new Tache();
        cible.setId(2L);

        DependanceRequest request = new DependanceRequest();
        request.setIdTacheCible(2L);
        request.setTypeLien(TypeLien.FIN_DEBUT);

        when(tacheRepository.findById(1L)).thenReturn(Optional.of(source));
        when(tacheRepository.findById(2L)).thenReturn(Optional.of(cible));
        when(tachePrecedenceRepository.existsByTacheSourceIdAndTacheCibleId(1L, 2L)).thenReturn(false);
        when(tachePrecedenceRepository.existsByTacheSourceIdAndTacheCibleId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(TachePrecedenceDejaExistanteException.class);
    }

    @Test
    void creer_utilisateurNonMembreDuProjet_neCreePasLaDependance() {
        Tache source = new Tache();
        source.setId(1L);
        DependanceRequest request = new DependanceRequest();
        request.setIdTacheCible(2L);
        request.setTypeLien(TypeLien.FIN_DEBUT);

        when(tacheRepository.findById(1L)).thenReturn(Optional.of(source));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(source.getProjet());

        assertThatThrownBy(() -> service.creer(1L, request))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(tachePrecedenceRepository, never()).save(any());
    }

    @Test
    void supprimer_relationInexistanteDansLesDeuxSens_leveTachePrecedenceIntrouvableException() {
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(new Tache()));
        when(tachePrecedenceRepository.existsById(new TachePrecedenceId(1L, 2L))).thenReturn(false);
        when(tachePrecedenceRepository.existsById(new TachePrecedenceId(2L, 1L))).thenReturn(false);

        assertThatThrownBy(() -> service.supprimer(1L, 2L))
                .isInstanceOf(TachePrecedenceIntrouvableException.class);
    }

    @Test
    void supprimer_relationExistanteEnSensInverse_supprimeLidInverse() {
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(new Tache()));
        when(tachePrecedenceRepository.existsById(new TachePrecedenceId(1L, 2L))).thenReturn(false);
        when(tachePrecedenceRepository.existsById(new TachePrecedenceId(2L, 1L))).thenReturn(true);

        service.supprimer(1L, 2L);

        verify(tachePrecedenceRepository).deleteById(new TachePrecedenceId(2L, 1L));
    }

    @Test
    void supprimer_utilisateurNonMembreDuProjet_neSupprimeRien() {
        Tache tache = new Tache();
        tache.setId(1L);
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(tache.getProjet());

        assertThatThrownBy(() -> service.supprimer(1L, 2L))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(tachePrecedenceRepository, never()).deleteById(any());
    }
}
