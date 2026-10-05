package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.service.ProjetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjetPlanificateur {

    private static final Logger log = LoggerFactory.getLogger(ProjetPlanificateur.class);

    private final ProjetService projetService;

    @Scheduled(cron = "0 0 0 * * *")
    public void verifierLesStatutsDesProjets() {
        log.info("Verification planifiee des statuts de projets demarree");
        projetService.demarrerLesProjetsArrivesADateDeDebut();
        log.info("Verification planifiee des statuts de projets terminee");
    }
}
