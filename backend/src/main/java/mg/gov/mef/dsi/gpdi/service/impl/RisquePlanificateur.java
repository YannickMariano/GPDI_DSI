package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.exception.AnalyseDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.service.DetectionRisqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lance chaque nuit la detection des risques sur les projets en cours, sans utilisateur a l'origine
 * de la demande. Volontairement sans transaction : chaque projet est traite par le service, qui gere
 * lui-meme ses transactions autour de l'appel (long) a l'assistant IA.
 */
@Component
@RequiredArgsConstructor
public class RisquePlanificateur {

    private static final Logger log = LoggerFactory.getLogger(RisquePlanificateur.class);

    private final ProjetRepository projetRepository;
    private final DetectionRisqueService detectionRisqueService;

    @Scheduled(cron = "${dsi.gpdi.ia.risque.cron}")
    public void detecterLesRisquesDesProjetsEnCours() {
        List<Long> idsProjets = new ArrayList<>();
        for (Projet projet : projetRepository.findAll()) {
            if (projet.getStatut() == StatutProjet.EN_COURS) {
                idsProjets.add(projet.getId());
            }
        }

        int analyses = 0;
        int echecs = 0;
        for (Long idProjet : idsProjets) {
            try {
                detectionRisqueService.detecter(idProjet, null);
                analyses++;
            } catch (AnalyseDejaEnCoursException e) {
                log.info("Projet {} ignore : une analyse est deja en cours", idProjet);
            } catch (OllamaIndisponibleException e) {
                // Inutile d'attendre le delai d'expiration pour chacun des projets restants.
                log.error("Assistant IA indisponible : detection nocturne interrompue au projet {} ({} projet(s) deja analyse(s) sur {})",
                        idProjet, analyses, idsProjets.size(), e);
                return;
            } catch (RuntimeException e) {
                echecs++;
                log.error("Detection des risques en echec pour le projet {}", idProjet, e);
            }
        }
        log.info("Detection nocturne des risques : {} projet(s) analyse(s), {} echec(s) sur {} projet(s) en cours",
                analyses, echecs, idsProjets.size());
    }
}
