package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPlanificateur {

    private static final Logger log = LoggerFactory.getLogger(NotificationPlanificateur.class);

    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 0 * * *")
    public void genererLesRappelsDEcheance() {
        log.info("Generation planifiee des rappels d'echeance demarree");
        notificationService.notifierEcheancesProches();
        log.info("Generation planifiee des rappels d'echeance terminee");
    }
}
