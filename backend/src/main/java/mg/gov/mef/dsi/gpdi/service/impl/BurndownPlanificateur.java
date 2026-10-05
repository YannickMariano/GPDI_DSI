package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.entity.BurndownSnapshot;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.repository.BurndownSnapshotRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Capture chaque jour le travail restant des sprints en cours, pour reconstituer une courbe de
 * burndown reelle. Necessaire car {@link mg.gov.mef.dsi.gpdi.entity.Tache#getTempsPasse()} n'est
 * qu'un cumul sans historique date.
 */
@Component
@RequiredArgsConstructor
public class BurndownPlanificateur {

    private static final Logger log = LoggerFactory.getLogger(BurndownPlanificateur.class);

    private final SprintRepository sprintRepository;
    private final TacheRepository tacheRepository;
    private final BurndownSnapshotRepository burndownSnapshotRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void capturerLesInstantanesDuJour() {
        LocalDate aujourdhui = LocalDate.now();
        List<Sprint> sprintsEnCours = sprintRepository.findByStatut(StatutSprint.EN_COURS);
        for (Sprint sprint : sprintsEnCours) {
            if (burndownSnapshotRepository.existsBySprintIdAndDate(sprint.getId(), aujourdhui)) {
                continue;
            }
            double heuresRestantes = tacheRepository.findBySprintId(sprint.getId()).stream()
                    .mapToDouble(this::heuresRestantesDeLaTache)
                    .sum();

            BurndownSnapshot snapshot = new BurndownSnapshot();
            snapshot.setSprint(sprint);
            snapshot.setDate(aujourdhui);
            snapshot.setHeuresRestantes(heuresRestantes);
            burndownSnapshotRepository.save(snapshot);
        }
        log.info("Instantanes de burndown captures pour {} sprint(s) en cours", sprintsEnCours.size());
    }

    private double heuresRestantesDeLaTache(Tache tache) {
        double estime = tache.getTempsEstime() != null ? tache.getTempsEstime() : 0.0;
        double passe = tache.getTempsPasse() != null ? tache.getTempsPasse() : 0.0;
        return Math.max(0, estime - passe);
    }
}
