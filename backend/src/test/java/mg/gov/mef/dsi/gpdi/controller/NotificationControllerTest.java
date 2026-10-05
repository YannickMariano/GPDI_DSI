package mg.gov.mef.dsi.gpdi.controller;

import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Notification;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeNotification;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.NotificationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    private static final String MATRICULE_MOI = "95001";
    private static final String MATRICULE_AUTRE = "95002";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void proprietesBaseDeDonnees(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AffectationTacheRepository affectationTacheRepository;
    @Autowired
    private TacheRepository tacheRepository;
    @Autowired
    private ProjetRepository projetRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;

    private Utilisateur moi;
    private Utilisateur autre;
    private Projet projet;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        notificationRepository.deleteAll();
        affectationTacheRepository.deleteAll();
        tacheRepository.deleteAll();
        projetRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("membre_equipe");
        role.setDescription("Membre d'equipe");
        Role roleEnregistre = roleRepository.save(role);

        moi = creerUtilisateur(MATRICULE_MOI, roleEnregistre);
        autre = creerUtilisateur(MATRICULE_AUTRE, roleEnregistre);

        Projet nouveauProjet = new Projet();
        nouveauProjet.setTitre("Projet Notification");
        nouveauProjet.setDateDebut(LocalDate.now());
        nouveauProjet.setDateFin(LocalDate.now().plusMonths(3));
        nouveauProjet.setPriorite(Priorite.NORMALE);
        nouveauProjet.setResponsable(autre);
        projet = projetRepository.save(nouveauProjet);
    }

    private Utilisateur creerUtilisateur(String matricule, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setMatricule(matricule);
        utilisateur.setNom("Nom" + matricule);
        utilisateur.setPrenom("Prenom");
        utilisateur.setEmail("u" + matricule + "@test.mg");
        utilisateur.setMotDePasse("HASH");
        utilisateur.setRole(role);
        return utilisateurRepository.save(utilisateur);
    }

    private Notification creerNotification(Utilisateur destinataire, String titre) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setType(TypeNotification.BLOCAGE);
        notification.setTitre(titre);
        notification.setMeta("Projet Notification");
        notification.setLien("/projets/" + projet.getId() + "/taches");
        return notificationRepository.save(notification);
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor connecteEnTantQueMoi() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("preferred_username", MATRICULE_MOI));
    }

    @Test
    void rechercher_retourneMesNotificationsDuPlusRecentAuPlusAncienSansCellesDesAutres() throws Exception {
        creerNotification(moi, "Ancienne");
        Thread.sleep(20);
        creerNotification(moi, "Recente");
        creerNotification(autre, "Pour quelqu'un d'autre");

        mockMvc.perform(get("/api/notifications").with(connecteEnTantQueMoi()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].titre").value("Recente"))
                .andExpect(jsonPath("$.content[1].titre").value("Ancienne"))
                .andExpect(jsonPath("$.content[0].type").value("BLOCAGE"))
                .andExpect(jsonPath("$.content[0].lu").value(false))
                .andExpect(jsonPath("$.content[0].lien").value("/projets/" + projet.getId() + "/taches"));
    }

    @Test
    void lire_maNotification_laMarqueLue() throws Exception {
        Notification notification = creerNotification(moi, "A lire");

        mockMvc.perform(post("/api/notifications/{id}/lire", notification.getId()).with(connecteEnTantQueMoi()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lu").value(true));

        mockMvc.perform(get("/api/notifications").with(connecteEnTantQueMoi()))
                .andExpect(jsonPath("$.content[0].lu").value(true));
    }

    @Test
    void lire_notificationDUnAutreUtilisateur_retourne404EtNeLaModifiePas() throws Exception {
        Notification notification = creerNotification(autre, "Pas pour moi");

        mockMvc.perform(post("/api/notifications/{id}/lire", notification.getId()).with(connecteEnTantQueMoi()))
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertFalse(notificationRepository.findById(notification.getId()).get().getLu());
    }

    @Test
    void lireToutes_marqueToutesMesNotificationsLuesSansToucherCellesDesAutres() throws Exception {
        creerNotification(moi, "Une");
        creerNotification(moi, "Deux");
        Notification pourAutre = creerNotification(autre, "Autre");

        mockMvc.perform(post("/api/notifications/lire-toutes").with(connecteEnTantQueMoi()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications").with(connecteEnTantQueMoi()))
                .andExpect(jsonPath("$.content[0].lu").value(true))
                .andExpect(jsonPath("$.content[1].lu").value(true));
        org.junit.jupiter.api.Assertions.assertFalse(notificationRepository.findById(pourAutre.getId()).get().getLu());
    }

    @Test
    void rechercher_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void echeancesProches_tacheDemainAssignee_creeUneSeuleNotificationMemeSiLeJobRepasse() throws Exception {
        Tache tache = new Tache();
        tache.setTitre("Livrer le lot");
        tache.setCode("T-1");
        tache.setPriorite(Priorite.HAUTE);
        tache.setDateFinPrevue(LocalDate.now().plusDays(1));
        tache.setProjet(projet);
        Tache tacheEnregistree = tacheRepository.save(tache);
        AffectationTache affectation = new AffectationTache();
        affectation.setTache(tacheEnregistree);
        affectation.setUtilisateur(moi);
        affectationTacheRepository.save(affectation);

        notificationService.notifierEcheancesProches();
        notificationService.notifierEcheancesProches();

        mockMvc.perform(get("/api/notifications").with(connecteEnTantQueMoi()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("ECHEANCE"))
                .andExpect(jsonPath("$.content[0].titre").value("T-1 — Livrer le lot : échéance demain"))
                .andExpect(jsonPath("$.content[0].meta").value("Projet Notification"));
    }
}
