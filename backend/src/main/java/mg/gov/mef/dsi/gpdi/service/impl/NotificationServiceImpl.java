package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.NotificationResponse;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.Notification;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeNotification;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.NotificationIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.NotificationMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.NotificationRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final int LONGUEUR_MAX_LIBELLE = 120;

    /** Les taches d'un projet dans l'un de ces statuts ne generent plus de rappel d'echeance. */
    private static final Set<StatutProjet> STATUTS_PROJET_SANS_RAPPEL =
            EnumSet.of(StatutProjet.SUSPENDU, StatutProjet.CLOTURE, StatutProjet.ARCHIVE);

    private final NotificationRepository notificationRepository;
    private final TacheRepository tacheRepository;
    private final AffectationTacheRepository affectationTacheRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> rechercherPourUtilisateur(Long idUtilisateur, Pageable pageable) {
        return notificationRepository.findByDestinataireIdOrderByDateCreationDesc(idUtilisateur, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse marquerCommeLue(Long idUtilisateur, Long id) {
        Notification notification = notificationRepository.findByIdAndDestinataireId(id, idUtilisateur)
                .orElseThrow(() -> new NotificationIntrouvableException(id));
        notification.setLu(true);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void marquerToutesCommeLues(Long idUtilisateur) {
        int nombre = notificationRepository.marquerToutesCommeLues(idUtilisateur);
        log.info("{} notification(s) marquee(s) lue(s) pour l'utilisateur {}", nombre, idUtilisateur);
    }

    @Override
    @Transactional
    public void notifierBlocage(Tache tache, Collection<Utilisateur> assignes) {
        Projet projet = tache.getProjet();
        if (projet == null) {
            return;
        }
        List<Utilisateur> destinataires = new ArrayList<>(assignes);
        destinataires.add(projet.getResponsable());
        notifier(destinataires, TypeNotification.BLOCAGE, libelle(tache) + " est bloquée",
                projet.getTitre(), lienTaches(projet), null);
    }

    @Override
    @Transactional
    public void notifierJalonEnRetard(Jalon jalon) {
        notifierResponsableDuJalon(jalon, "Jalon « " + abreger(jalon.getTitre()) + " » en retard", "JALON_RETARD:" + jalon.getId());
    }

    @Override
    @Transactional
    public void notifierJalonAtteint(Jalon jalon) {
        notifierResponsableDuJalon(jalon, "Jalon « " + abreger(jalon.getTitre()) + " » atteint", "JALON_ATTEINT:" + jalon.getId());
    }

    @Override
    @Transactional
    public void notifierEcheancesProches() {
        LocalDate aujourdhui = LocalDate.now();
        List<Tache> taches = tacheRepository.findByDateFinPrevueLessThanEqualAndStatutNot(
                aujourdhui.plusDays(1), StatutTache.TERMINEE);
        for (Tache tache : taches) {
            Projet projet = tache.getProjet();
            if (projet == null || STATUTS_PROJET_SANS_RAPPEL.contains(projet.getStatut())) {
                continue;
            }
            List<Utilisateur> assignes = affectationTacheRepository.findByTacheId(tache.getId()).stream()
                    .map(AffectationTache::getUtilisateur)
                    .collect(Collectors.toList());
            notifier(assignes, TypeNotification.ECHEANCE, libelle(tache) + " : " + echeance(tache.getDateFinPrevue(), aujourdhui),
                    projet.getTitre(), lienTaches(projet), "ECHEANCE:" + tache.getId());
        }
        log.info("Rappels d'echeance evalues pour {} tache(s)", taches.size());
    }

    private void notifierResponsableDuJalon(Jalon jalon, String titre, String cle) {
        Projet projet = jalon.getProjet();
        if (projet == null) {
            return;
        }
        notifier(Collections.singletonList(projet.getResponsable()), TypeNotification.JALON, titre,
                projet.getTitre(), "/projets/" + projet.getId() + "/jalons", cle);
    }

    /** Cree une notification par destinataire distinct (les absents sont ignores) ; une cle deja recue est ignoree. */
    private void notifier(Collection<Utilisateur> destinataires, TypeNotification type, String titre, String meta,
                          String lien, String cle) {
        Set<Long> dejaTraites = new HashSet<>();
        for (Utilisateur destinataire : destinataires) {
            if (destinataire == null || destinataire.getId() == null || !dejaTraites.add(destinataire.getId())) {
                continue;
            }
            if (cle != null && notificationRepository.existsByDestinataireIdAndCle(destinataire.getId(), cle)) {
                continue;
            }
            Notification notification = new Notification();
            notification.setDestinataire(destinataire);
            notification.setType(type);
            notification.setTitre(titre);
            notification.setMeta(meta);
            notification.setLien(lien);
            notification.setCle(cle);
            notificationRepository.save(notification);
        }
    }

    private String echeance(LocalDate echeance, LocalDate aujourdhui) {
        if (echeance.isBefore(aujourdhui)) {
            return "échéance dépassée";
        }
        return echeance.isEqual(aujourdhui) ? "échéance aujourd'hui" : "échéance demain";
    }

    private String libelle(Tache tache) {
        return tache.getCode() + " — " + abreger(tache.getTitre());
    }

    private String lienTaches(Projet projet) {
        return "/projets/" + projet.getId() + "/taches";
    }

    private String abreger(String texte) {
        if (texte == null || texte.length() <= LONGUEUR_MAX_LIBELLE) {
            return texte;
        }
        return texte.substring(0, LONGUEUR_MAX_LIBELLE - 1) + "…";
    }
}
