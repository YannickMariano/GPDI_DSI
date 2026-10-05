package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.service.JalonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JalonPlanificateur {

    private static final Logger log = LoggerFactory.getLogger(JalonPlanificateur.class);

    private final JalonService jalonService;

    @Scheduled(cron = "0 0 0 * * *")
    public void verifierLesStatutsDesJalons() {
        log.info("Verification planifiee des statuts de jalons demarree");
        jalonService.marquerEnRetardLesJalonsEnRetard();
        log.info("Verification planifiee des statuts de jalons terminee");
    }
}
