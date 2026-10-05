package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.entity.BurndownSnapshot;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.repository.BurndownSnapshotRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.service.impl.BurndownPlanificateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BurndownPlanificateurTest {

    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private BurndownSnapshotRepository burndownSnapshotRepository;

    private BurndownPlanificateur planificateur;

    @BeforeEach
    void initPlanificateur() {
        planificateur = new BurndownPlanificateur(sprintRepository, tacheRepository, burndownSnapshotRepository);
    }

    @Test
    void capturerLesInstantanesDuJour_sprintEnCoursSansInstantane_creeUnInstantane() {
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        when(sprintRepository.findByStatut(StatutSprint.EN_COURS)).thenReturn(Collections.singletonList(sprint));
        when(burndownSnapshotRepository.existsBySprintIdAndDate(10L, LocalDate.now())).thenReturn(false);

        Tache tacheAvancee = new Tache();
        tacheAvancee.setTempsEstime(10.0);
        tacheAvancee.setTempsPasse(4.0);
        Tache tacheDepassee = new Tache();
        tacheDepassee.setTempsEstime(5.0);
        tacheDepassee.setTempsPasse(8.0); // depassement : ne doit pas rendre le restant negatif
        when(tacheRepository.findBySprintId(10L)).thenReturn(Arrays.asList(tacheAvancee, tacheDepassee));

        planificateur.capturerLesInstantanesDuJour();

        ArgumentCaptor<BurndownSnapshot> captor = ArgumentCaptor.forClass(BurndownSnapshot.class);
        verify(burndownSnapshotRepository).save(captor.capture());
        BurndownSnapshot snapshot = captor.getValue();
        assertThat(snapshot.getSprint()).isEqualTo(sprint);
        assertThat(snapshot.getDate()).isEqualTo(LocalDate.now());
        assertThat(snapshot.getHeuresRestantes()).isEqualTo(6.0); // (10-4) + max(0, 5-8)
    }

    @Test
    void capturerLesInstantanesDuJour_instantaneDejaExistant_neDupliquePas() {
        Sprint sprint = new Sprint();
        sprint.setId(10L);
        when(sprintRepository.findByStatut(StatutSprint.EN_COURS)).thenReturn(Collections.singletonList(sprint));
        when(burndownSnapshotRepository.existsBySprintIdAndDate(10L, LocalDate.now())).thenReturn(true);

        planificateur.capturerLesInstantanesDuJour();

        verify(burndownSnapshotRepository, never()).save(any(BurndownSnapshot.class));
    }
}
