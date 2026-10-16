package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.JalonRequest;
import mg.gov.mef.dsi.gpdi.dto.response.JalonResponse;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutJalon;
import mg.gov.mef.dsi.gpdi.entity.StatutLivrable;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.exception.DateHorsPeriodeProjetException;
import mg.gov.mef.dsi.gpdi.exception.JalonIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.LivrableEnAttenteException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.ProjetNonDemarreException;
import mg.gov.mef.dsi.gpdi.exception.StatutJalonInvalideException;
import mg.gov.mef.dsi.gpdi.mapper.JalonMapper;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.LivrableRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import mg.gov.mef.dsi.gpdi.service.JalonService;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JalonServiceImpl implements JalonService {

    private static final Logger log = LoggerFactory.getLogger(JalonServiceImpl.class);

    private static final String FONCTION_JALONS_LIVRABLES = "Gestion des jalons et livrables";

    private static final Set<StatutJalon> STATUTS_NON_TERMINAUX =
            EnumSet.of(StatutJalon.A_VENIR, StatutJalon.EN_RETARD);

    private final JalonRepository jalonRepository;
    private final ProjetRepository projetRepository;
    private final JalonMapper jalonMapper;
    private final LivrableRepository livrableRepository;
    private final NotificationService notificationService;
    private final AccesProjetService accesProjetService;
    private final HabilitationService habilitationService;

    @Override
    @Transactional
    public JalonResponse creer(Long idProjet, JalonRequest request) {
        Projet projet = obtenirProjet(idProjet);
        accesProjetService.verifierMembreDuProjet(projet);
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        verifierDateDansLePerimetreDuProjet(projet, request.getDateEcheance());

        Jalon jalon = jalonMapper.toEntity(request);
        jalon.setProjet(projet);

        JalonResponse reponse = jalonMapper.toResponse(jalonRepository.save(jalon));
        log.info("Jalon {} cree pour le projet {}", reponse.getId(), idProjet);
        return reponse;
    }

    @Override
    @Transactional
    public Page<JalonResponse> rechercher(Long idProjet, Pageable pageable) {
        Projet projet = obtenirProjet(idProjet);
        accesProjetService.verifierAccesLecturePartageable(projet);
        Page<Jalon> jalons = jalonRepository.findByProjetId(idProjet, pageable);
        jalons.forEach(this::marquerEnRetardSiNecessaire);
        return jalons.map(jalonMapper::toResponse);
    }

    @Override
    @Transactional
    public JalonResponse obtenir(Long idProjet, Long id) {
        Jalon jalon = obtenirJalonDuProjet(idProjet, id);
        accesProjetService.verifierAccesLecturePartageable(jalon.getProjet());
        marquerEnRetardSiNecessaire(jalon);
        return jalonMapper.toResponse(jalon);
    }

    @Override
    @Transactional
    public JalonResponse modifier(Long idProjet, Long id, JalonRequest request) {
        Jalon jalon = obtenirJalonDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(jalon.getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        if (jalon.getStatut() == StatutJalon.ATTEINT || jalon.getStatut() == StatutJalon.ANNULER) {
            throw new StatutJalonInvalideException(id, jalon.getStatut(), "modifie");
        }
        verifierDateDansLePerimetreDuProjet(jalon.getProjet(), request.getDateEcheance());
        jalonMapper.updateEntity(request, jalon);

        JalonResponse reponse = jalonMapper.toResponse(jalonRepository.save(jalon));
        log.info("Jalon {} modifie", id);
        return reponse;
    }

    @Override
    @Transactional
    public void supprimer(Long idProjet, Long id) {
        Jalon jalon = obtenirJalonDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(jalon.getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        jalonRepository.delete(jalon);
        log.info("Jalon {} supprime", id);
    }

    @Override
    @Transactional
    public JalonResponse terminer(Long idProjet, Long id) {
        Jalon jalon = obtenirJalonDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(jalon.getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        if (!STATUTS_NON_TERMINAUX.contains(jalon.getStatut())) {
            throw new StatutJalonInvalideException(id, jalon.getStatut(), "marque comme atteint");
        }
        if (jalon.getProjet().getStatut() == StatutProjet.EN_PREPARATION) {
            throw new ProjetNonDemarreException("cloturer ce jalon");
        }
        if (livrableRepository.existsByJalonIdAndStatut(id, StatutLivrable.ATTENDU)) {
            throw new LivrableEnAttenteException(id);
        }
        jalon.setStatut(StatutJalon.ATTEINT);
        jalon.setDateValidation(LocalDate.now());

        JalonResponse reponse = jalonMapper.toResponse(jalonRepository.save(jalon));
        notificationService.notifierJalonAtteint(jalon);
        log.info("Jalon {} atteint", id);
        return reponse;
    }

    @Override
    @Transactional
    public JalonResponse annuler(Long idProjet, Long id) {
        Jalon jalon = obtenirJalonDuProjet(idProjet, id);
        accesProjetService.verifierMembreDuProjet(jalon.getProjet());
        habilitationService.verifierEcriture(FONCTION_JALONS_LIVRABLES);
        if (!STATUTS_NON_TERMINAUX.contains(jalon.getStatut())) {
            throw new StatutJalonInvalideException(id, jalon.getStatut(), "annule");
        }
        jalon.setStatut(StatutJalon.ANNULER);

        JalonResponse reponse = jalonMapper.toResponse(jalonRepository.save(jalon));
        log.info("Jalon {} annule", id);
        return reponse;
    }

    @Override
    @Transactional
    public void marquerEnRetardLesJalonsEnRetard() {
        List<Jalon> jalons = jalonRepository.findByStatutAndDateEcheanceLessThan(
                StatutJalon.A_VENIR, LocalDate.now());
        jalons.forEach(this::marquerEnRetardSiNecessaire);
    }

    /**
     * Bascule un jalon non livre dont l'echeance est depassee vers EN_RETARD. Appele a la fois par la
     * verification planifiee (minuit) et a chaque lecture, pour que le changement soit visible sans
     * attendre le prochain passage du planificateur.
     */
    private void marquerEnRetardSiNecessaire(Jalon jalon) {
        if (jalon.getStatut() == StatutJalon.A_VENIR && jalon.getDateEcheance() != null && jalon.getDateEcheance().isBefore(LocalDate.now())) {
            jalon.setStatut(StatutJalon.EN_RETARD);
            jalonRepository.save(jalon);
            notificationService.notifierJalonEnRetard(jalon);
            log.info("Jalon {} marque EN_RETARD (echeance depassee)", jalon.getId());
        }
    }

    /** Un jalon ne peut pas echoir apres la date de fin de son projet. */
    private void verifierDateDansLePerimetreDuProjet(Projet projet, LocalDate dateEcheance) {
        if (projet.getDateFin() != null && dateEcheance.isAfter(projet.getDateFin())) {
            throw new DateHorsPeriodeProjetException(projet.getDateFin());
        }
    }

    private Projet obtenirProjet(Long idProjet) {
        return projetRepository.findById(idProjet)
                .orElseThrow(() -> new ProjetIntrouvableException(idProjet));
    }

    private Jalon obtenirJalonDuProjet(Long idProjet, Long id) {
        Jalon jalon = jalonRepository.findById(id)
                .orElseThrow(() -> new JalonIntrouvableException(id));
        if (jalon.getProjet() == null || !jalon.getProjet().getId().equals(idProjet)) {
            throw new JalonIntrouvableException(id);
        }
        return jalon;
    }
}
