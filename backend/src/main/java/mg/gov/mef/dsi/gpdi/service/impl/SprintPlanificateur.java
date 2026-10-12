package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.service.SprintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintPlanificateur {

    private static final Logger log = LoggerFactory.getLogger(SprintPlanificateur.class);

    private final SprintService sprintService;

    @Scheduled(cron = "0 0 0 * * *")
    public void demarrerLesSprintsArrivesADateDeDebut() {
        log.info("Demarrage planifie des sprints arrives a date de debut demarre");
        sprintService.demarrerLesSprintsArrivesADateDeDebut();
        log.info("Demarrage planifie des sprints arrives a date de debut termine");
    }
}
