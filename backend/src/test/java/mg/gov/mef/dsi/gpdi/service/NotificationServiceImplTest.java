package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.response.NotificationResponse;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.Notification;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.RapportDiffuse;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeNotification;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.NotificationIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.NotificationMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.NotificationRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private AffectationTacheRepository affectationTacheRepository;
    @Mock
    private MembreEquipeRepository membreEquipeRepository;
    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl service;

    @BeforeEach
    void initService() {
        service = new NotificationServiceImpl(notificationRepository, tacheRepository, affectationTacheRepository,
                membreEquipeRepository, notificationMapper);
    }

    private Utilisateur utilisateur(Long id) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        return utilisateur;
    }

    private Projet projet(Utilisateur responsable) {
        Projet projet = new Projet();
        projet.setId(1L);
        projet.setTitre("Portail des marchés");
        projet.setResponsable(responsable);
        projet.setStatut(StatutProjet.EN_COURS);
        return projet;
    }

    private Tache tache(Projet projet, LocalDate echeance) {
        Tache tache = new Tache();
        tache.setId(10L);
        tache.setCode("T-10");
        tache.setTitre("Migrer la base");
        tache.setProjet(projet);
        tache.setDateFinPrevue(echeance);
        return tache;
    }

    private Equipe equipe(Long id) {
        Equipe equipe = new Equipe();
        equipe.setId(id);
        return equipe;
    }

    private MembreEquipe membre(Equipe equipe, Utilisateur utilisateur, boolean actif) {
        MembreEquipe membre = new MembreEquipe();
        membre.setEquipe(equipe);
        membre.setUtilisateur(utilisateur);
        membre.setActif(actif);
        return membre;
    }

    private Sprint sprint(Projet projet) {
        Sprint sprint = new Sprint();
        sprint.setId(20L);
        sprint.setNom("Sprint 3");
        sprint.setProjet(projet);
        return sprint;
    }

    private List<Notification> notificationsEnregistrees(int nombreAttendu) {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(nombreAttendu)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void marquerCommeLue_notificationDuDestinataire_laMarqueLue() {
        Notification notification = new Notification();
        notification.setId(5L);
        notification.setLu(false);
        when(notificationRepository.findByIdAndDestinataireId(5L, 1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toResponse(notification)).thenReturn(new NotificationResponse());

        service.marquerCommeLue(1L, 5L);

        assertThat(notification.getLu()).isTrue();
    }

    @Test
    void marquerCommeLue_notificationDUnAutreUtilisateur_leveNotificationIntrouvableException() {
        when(notificationRepository.findByIdAndDestinataireId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marquerCommeLue(1L, 5L))
                .isInstanceOf(NotificationIntrouvableException.class);
    }

    @Test
    void marquerToutesCommeLues_deleguePourLUtilisateurCourant() {
        service.marquerToutesCommeLues(1L);

        verify(notificationRepository).marquerToutesCommeLues(1L);
    }

    @Test
    void notifierBlocage_assignesEtResponsable_creeUneNotificationParPersonneDistincte() {
        Utilisateur responsable = utilisateur(1L);
        Utilisateur assigne = utilisateur(2L);
        Tache tache = tache(projet(responsable), null);

        // Le responsable est aussi assigne : il ne doit recevoir qu'une seule notification.
        service.notifierBlocage(tache, Arrays.asList(assigne, responsable));

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(creees.get(0).getType()).isEqualTo(TypeNotification.BLOCAGE);
        assertThat(creees.get(0).getTitre()).isEqualTo("T-10 — Migrer la base est bloquée");
        assertThat(creees.get(0).getMeta()).isEqualTo("Portail des marchés");
        assertThat(creees.get(0).getLien()).isEqualTo("/projets/1/taches");
        assertThat(creees.get(0).getCle()).isNull();
    }

    @Test
    void notifierBlocage_projetSansResponsable_neNotifieQueLesAssignes() {
        Tache tache = tache(projet(null), null);

        service.notifierBlocage(tache, Collections.singletonList(utilisateur(2L)));

        assertThat(notificationsEnregistrees(1).get(0).getDestinataire().getId()).isEqualTo(2L);
    }

    @Test
    void notifierJalonAtteint_responsableDuProjet_creeUneNotificationJalonAvecCle() {
        Jalon jalon = new Jalon();
        jalon.setId(7L);
        jalon.setTitre("Bascule du référentiel");
        jalon.setProjet(projet(utilisateur(1L)));

        service.notifierJalonAtteint(jalon);

        Notification creee = notificationsEnregistrees(1).get(0);
        assertThat(creee.getType()).isEqualTo(TypeNotification.JALON);
        assertThat(creee.getTitre()).isEqualTo("Jalon « Bascule du référentiel » atteint");
        assertThat(creee.getLien()).isEqualTo("/projets/1/jalons");
        assertThat(creee.getCle()).isEqualTo("JALON_ATTEINT:7");
    }

    @Test
    void notifierJalonEnRetard_cleDejaRecue_neCreeRien() {
        Jalon jalon = new Jalon();
        jalon.setId(7L);
        jalon.setTitre("Bascule");
        jalon.setProjet(projet(utilisateur(1L)));
        when(notificationRepository.existsByDestinataireIdAndCle(1L, "JALON_RETARD:7")).thenReturn(true);

        service.notifierJalonEnRetard(jalon);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void notifierEcheancesProches_tacheDemain_notifieLesAssignesAvecCleDeDeduplication() {
        Tache tache = tache(projet(utilisateur(1L)), LocalDate.now().plusDays(1));
        AffectationTache affectation = new AffectationTache();
        affectation.setUtilisateur(utilisateur(2L));
        when(tacheRepository.findByDateFinPrevueLessThanEqualAndStatutNot(LocalDate.now().plusDays(1), StatutTache.TERMINEE))
                .thenReturn(Collections.singletonList(tache));
        when(affectationTacheRepository.findByTacheId(10L)).thenReturn(Collections.singletonList(affectation));

        service.notifierEcheancesProches();

        Notification creee = notificationsEnregistrees(1).get(0);
        assertThat(creee.getDestinataire().getId()).isEqualTo(2L);
        assertThat(creee.getType()).isEqualTo(TypeNotification.ECHEANCE);
        assertThat(creee.getTitre()).isEqualTo("T-10 — Migrer la base : échéance demain");
        assertThat(creee.getCle()).isEqualTo("ECHEANCE:10");
    }

    @Test
    void notifierEcheancesProches_tacheEnRetard_titreIndiqueEcheanceDepassee() {
        Tache tache = tache(projet(utilisateur(1L)), LocalDate.now().minusDays(3));
        AffectationTache affectation = new AffectationTache();
        affectation.setUtilisateur(utilisateur(2L));
        when(tacheRepository.findByDateFinPrevueLessThanEqualAndStatutNot(LocalDate.now().plusDays(1), StatutTache.TERMINEE))
                .thenReturn(Collections.singletonList(tache));
        when(affectationTacheRepository.findByTacheId(10L)).thenReturn(Collections.singletonList(affectation));

        service.notifierEcheancesProches();

        assertThat(notificationsEnregistrees(1).get(0).getTitre()).isEqualTo("T-10 — Migrer la base : échéance dépassée");
    }

    @Test
    void notifierEcheancesProches_projetCloture_neNotifiePas() {
        Projet projetCloture = projet(utilisateur(1L));
        projetCloture.setStatut(StatutProjet.CLOTURE);
        Tache tache = tache(projetCloture, LocalDate.now().minusDays(3));
        when(tacheRepository.findByDateFinPrevueLessThanEqualAndStatutNot(LocalDate.now().plusDays(1), StatutTache.TERMINEE))
                .thenReturn(Collections.singletonList(tache));

        service.notifierEcheancesProches();

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void notifierDemarrageSprint_membresActifsDeLEquipe_sontNotifiesAvecCleDeDeduplication() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        Sprint sprint = sprint(projet);
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Arrays.asList(
                membre(equipe, utilisateur(2L), true),
                membre(equipe, utilisateur(4L), false)));

        service.notifierDemarrageSprint(sprint);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        Notification creee = creees.get(0);
        assertThat(creee.getType()).isEqualTo(TypeNotification.SPRINT);
        assertThat(creee.getTitre()).isEqualTo("Sprint « Sprint 3 » démarré");
        assertThat(creee.getLien()).isEqualTo("/projets/1/backlog");
        assertThat(creee.getCle()).isEqualTo("SPRINT_DEMARRE:20");
    }

    @Test
    void notifierClotureSprint_membresActifsDeLEquipe_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        Sprint sprint = sprint(projet);
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierClotureSprint(sprint);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        Notification creee = creees.get(0);
        assertThat(creee.getType()).isEqualTo(TypeNotification.SPRINT);
        assertThat(creee.getTitre()).isEqualTo("Sprint « Sprint 3 » clôturé");
        assertThat(creee.getCle()).isEqualTo("SPRINT_CLOTURE:20");
    }

    @Test
    void notifierDiffusionRapport_membresActifsDeLEquipe_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        Utilisateur auteur = utilisateur(1L);
        auteur.setPrenom("Lala");
        auteur.setNom("Rabe");
        RapportDiffuse rapport = new RapportDiffuse();
        rapport.setId(30L);
        rapport.setProjet(projet);
        rapport.setAuteur(auteur);
        rapport.setNom("Rapport hebdo");
        rapport.setTitre("Semaine 42");
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierDiffusionRapport(rapport);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        Notification creee = creees.get(0);
        assertThat(creee.getType()).isEqualTo(TypeNotification.RAPPORT);
        assertThat(creee.getTitre()).isEqualTo("Nouveau rapport diffusé par Lala Rabe : Semaine 42");
        assertThat(creee.getLien()).isEqualTo("/diffusions");
    }

    @Test
    void notifierAffectation_nouveauxAssignes_sontNotifies() {
        Tache tache = tache(projet(utilisateur(1L)), null);

        service.notifierAffectation(tache, Collections.singletonList(utilisateur(2L)));

        Notification creee = notificationsEnregistrees(1).get(0);
        assertThat(creee.getDestinataire().getId()).isEqualTo(2L);
        assertThat(creee.getType()).isEqualTo(TypeNotification.AFFECTATION);
        assertThat(creee.getTitre()).isEqualTo("Vous avez été affecté à T-10 — Migrer la base");
        assertThat(creee.getLien()).isEqualTo("/projets/1/taches");
    }

    @Test
    void notifierAffectation_aucunNouvelAssigne_neCreeRien() {
        Tache tache = tache(projet(utilisateur(1L)), null);

        service.notifierAffectation(tache, Collections.emptyList());

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void notifierDemarrageTache_membresActifsDeLEquipeEtResponsable_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        Tache tache = tache(projet, null);
        Utilisateur acteur = utilisateur(5L);
        acteur.setPrenom("Lala");
        acteur.setNom("Rabe");
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierDemarrageTache(tache, acteur);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(creees.get(0).getType()).isEqualTo(TypeNotification.TACHE);
        assertThat(creees.get(0).getTitre()).isEqualTo("T-10 — Migrer la base démarrée par Lala Rabe");
        assertThat(creees.get(0).getLien()).isEqualTo("/projets/1/taches");
    }

    @Test
    void notifierClotureTache_membresActifsDeLEquipeEtResponsable_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        Tache tache = tache(projet, null);
        Utilisateur acteur = utilisateur(5L);
        acteur.setPrenom("Lala");
        acteur.setNom("Rabe");
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierClotureTache(tache, acteur);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(creees.get(0).getType()).isEqualTo(TypeNotification.TACHE);
        assertThat(creees.get(0).getTitre()).isEqualTo("T-10 — Migrer la base terminée par Lala Rabe");
    }

    @Test
    void notifierDemarrageProjet_membresActifsDeLEquipeEtResponsable_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierDemarrageProjet(projet);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(creees.get(0).getType()).isEqualTo(TypeNotification.PROJET);
        assertThat(creees.get(0).getTitre()).isEqualTo("Projet « Portail des marchés » démarré");
        assertThat(creees.get(0).getLien()).isEqualTo("/projets/1/apercu");
    }

    @Test
    void notifierClotureProjet_membresActifsDeLEquipeEtResponsable_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierClotureProjet(projet);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(creees.get(0).getType()).isEqualTo(TypeNotification.PROJET);
        assertThat(creees.get(0).getTitre()).isEqualTo("Projet « Portail des marchés » clôturé");
    }

    @Test
    void notifierSuspensionProjet_membresActifsDeLEquipeEtResponsable_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierSuspensionProjet(projet);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(creees.get(0).getType()).isEqualTo(TypeNotification.PROJET);
        assertThat(creees.get(0).getTitre()).isEqualTo("Projet « Portail des marchés » suspendu");
    }

    @Test
    void notifierRepriseProjet_membresActifsDeLEquipeEtResponsable_sontNotifies() {
        Equipe equipe = equipe(3L);
        Projet projet = projet(utilisateur(1L));
        projet.setEquipe(equipe);
        when(membreEquipeRepository.findByEquipeId(3L)).thenReturn(Collections.singletonList(membre(equipe, utilisateur(2L), true)));

        service.notifierRepriseProjet(projet);

        List<Notification> creees = notificationsEnregistrees(2);
        assertThat(creees).extracting(n -> n.getDestinataire().getId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(creees.get(0).getType()).isEqualTo(TypeNotification.PROJET);
        assertThat(creees.get(0).getTitre()).isEqualTo("Projet « Portail des marchés » repris");
    }
}
