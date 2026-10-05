package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.SousTacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SousTacheResponse;
import mg.gov.mef.dsi.gpdi.entity.SousTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.exception.SousTacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.TacheMapper;
import mg.gov.mef.dsi.gpdi.repository.SousTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.impl.SousTacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SousTacheServiceImplTest {

    @Mock
    private SousTacheRepository sousTacheRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private TacheMapper tacheMapper;
    @Mock
    private AccesProjetService accesProjetService;

    private SousTacheServiceImpl service;

    @BeforeEach
    void initService() {
        service = new SousTacheServiceImpl(sousTacheRepository, tacheRepository, tacheMapper, accesProjetService);
    }

    @Test
    void creer_tacheExistante_associeLaSousTache() {
        Tache tache = new Tache();
        tache.setId(1L);
        SousTacheRequest request = new SousTacheRequest();
        request.setTitre("Ecrire les tests");

        SousTache sousTacheEnregistree = new SousTache();
        sousTacheEnregistree.setId(10L);

        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(sousTacheRepository.save(org.mockito.ArgumentMatchers.any(SousTache.class)))
                .thenReturn(sousTacheEnregistree);
        when(tacheMapper.toSousTacheResponse(sousTacheEnregistree))
                .thenReturn(new SousTacheResponse(10L, "Ecrire les tests", false));

        SousTacheResponse reponse = service.creer(1L, request);

        assertThat(reponse.getId()).isEqualTo(10L);
    }

    @Test
    void creer_tacheInexistante_leveTacheIntrouvableException() {
        when(tacheRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(999L, new SousTacheRequest()))
                .isInstanceOf(TacheIntrouvableException.class);
    }

    @Test
    void creer_utilisateurNonMembreDuProjet_neCreePasLaSousTache() {
        Tache tache = new Tache();
        tache.setId(1L);
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(tache.getProjet());

        assertThatThrownBy(() -> service.creer(1L, new SousTacheRequest()))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(sousTacheRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void supprimer_sousTacheAppartenantAUneAutreTache_leveSousTacheIntrouvableException() {
        Tache autreTache = new Tache();
        autreTache.setId(2L);
        SousTache sousTache = new SousTache();
        sousTache.setId(10L);
        sousTache.setTache(autreTache);

        when(sousTacheRepository.findById(10L)).thenReturn(Optional.of(sousTache));

        assertThatThrownBy(() -> service.supprimer(1L, 10L))
                .isInstanceOf(SousTacheIntrouvableException.class);
    }

    @Test
    void supprimer_sousTacheDeLaBonneTache_appelleDelete() {
        Tache tache = new Tache();
        tache.setId(1L);
        SousTache sousTache = new SousTache();
        sousTache.setId(10L);
        sousTache.setTache(tache);

        when(sousTacheRepository.findById(10L)).thenReturn(Optional.of(sousTache));

        service.supprimer(1L, 10L);

        verify(sousTacheRepository).delete(sousTache);
    }

    @Test
    void basculer_sousTacheNonFaite_laMarqueFaite() {
        Tache tache = new Tache();
        tache.setId(1L);
        SousTache sousTache = new SousTache();
        sousTache.setId(10L);
        sousTache.setFait(false);
        sousTache.setTache(tache);

        when(sousTacheRepository.findById(10L)).thenReturn(Optional.of(sousTache));
        when(sousTacheRepository.save(sousTache)).thenReturn(sousTache);
        when(tacheMapper.toSousTacheResponse(sousTache)).thenReturn(new SousTacheResponse(10L, "Ecrire les tests", true));

        SousTacheResponse reponse = service.basculer(1L, 10L);

        assertThat(reponse.getFait()).isTrue();
        assertThat(sousTache.getFait()).isTrue();
    }
}
