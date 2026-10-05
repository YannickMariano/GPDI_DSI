package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.BurndownPointResponse;
import mg.gov.mef.dsi.gpdi.entity.BurndownSnapshot;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.exception.SprintIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.BurndownSnapshotRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.service.impl.BurndownServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BurndownServiceImplTest {

    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private BurndownSnapshotRepository burndownSnapshotRepository;

    private BurndownServiceImpl service;

    @BeforeEach
    void initService() {
        service = new BurndownServiceImpl(sprintRepository, tacheRepository, burndownSnapshotRepository);
    }

    private Sprint sprintValide() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        sprint.setDateDebut(LocalDate.of(2026, 8, 1));
        sprint.setDateFin(LocalDate.of(2026, 8, 5));
        return sprint;
    }

    @Test
    void obtenir_sprintInexistant_leveSprintIntrouvableException() {
        when(sprintRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(1L, 999L))
                .isInstanceOf(SprintIntrouvableException.class);
    }

    @Test
    void obtenir_sprintDunAutreProjet_leveSprintIntrouvableException() {
        Sprint sprint = sprintValide();
        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        assertThatThrownBy(() -> service.obtenir(2L, 10L))
                .isInstanceOf(SprintIntrouvableException.class);
    }

    @Test
    void obtenir_sprintSansDates_retourneListeVide() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        List<BurndownPointResponse> points = service.obtenir(1L, 10L);

        assertThat(points).isEmpty();
    }

    @Test
    void obtenir_sprintValide_calculeIdealLineaireEtRestantDepuisLesInstantanes() {
        Sprint sprint = sprintValide();
        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));

        Tache tache1 = new Tache();
        tache1.setTempsEstime(20.0);
        Tache tache2 = new Tache();
        tache2.setTempsEstime(20.0);
        when(tacheRepository.findBySprintId(10L)).thenReturn(Arrays.asList(tache1, tache2));

        BurndownSnapshot snapshotJour0 = new BurndownSnapshot();
        snapshotJour0.setDate(LocalDate.of(2026, 8, 1));
        snapshotJour0.setHeuresRestantes(40.0);
        BurndownSnapshot snapshotJour1 = new BurndownSnapshot();
        snapshotJour1.setDate(LocalDate.of(2026, 8, 2));
        snapshotJour1.setHeuresRestantes(30.0);
        when(burndownSnapshotRepository.findBySprintIdOrderByDateAsc(10L))
                .thenReturn(Arrays.asList(snapshotJour0, snapshotJour1));

        List<BurndownPointResponse> points = service.obtenir(1L, 10L);

        assertThat(points).hasSize(5); // 01/08 au 05/08 inclus
        assertThat(points.get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(points.get(0).getIdeal()).isEqualTo(40.0);
        assertThat(points.get(0).getRestant()).isEqualTo(40.0);
        assertThat(points.get(1).getRestant()).isEqualTo(30.0);
        assertThat(points.get(4).getIdeal()).isEqualTo(0.0);
        assertThat(points.get(4).getRestant()).isNull(); // aucun instantane capture ce jour-la
    }

    @Test
    void obtenir_sprintDUnSeulJour_idealVautZero() {
        Projet projet = new Projet();
        projet.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        sprint.setProjet(projet);
        sprint.setDateDebut(LocalDate.of(2026, 8, 1));
        sprint.setDateFin(LocalDate.of(2026, 8, 1));
        when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
        when(tacheRepository.findBySprintId(10L)).thenReturn(Collections.emptyList());
        when(burndownSnapshotRepository.findBySprintIdOrderByDateAsc(10L)).thenReturn(Collections.emptyList());

        List<BurndownPointResponse> points = service.obtenir(1L, 10L);

        assertThat(points).hasSize(1);
        assertThat(points.get(0).getIdeal()).isEqualTo(0.0);
    }
}
