package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.PointInformationRequest;
import mg.gov.mef.dsi.gpdi.entity.PartageClient;
import mg.gov.mef.dsi.gpdi.entity.PointInformation;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import mg.gov.mef.dsi.gpdi.repository.PointInformationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
class PointInformationControllerTest {

    private static final String MATRICULE_CHEF_PROJET = "95001";
    private static final String MATRICULE_CLIENT = "95002";
    private static final String MATRICULE_SANS_ACCES = "95003";

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
    private ObjectMapper objectMapper;
    @Autowired
    private PointInformationRepository pointInformationRepository;
    @Autowired
    private PartageClientRepository partageClientRepository;
    @Autowired
    private ProjetRepository projetRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;

    private Utilisateur chefDeProjet;
    private Utilisateur client;
    private Utilisateur sansAcces;
    private Projet projet;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        pointInformationRepository.deleteAll();
        partageClientRepository.deleteAll();
        projetRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("chef_projet");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);

        chefDeProjet = creerUtilisateur(MATRICULE_CHEF_PROJET, roleEnregistre);
        client = creerUtilisateur(MATRICULE_CLIENT, roleEnregistre);
        sansAcces = creerUtilisateur(MATRICULE_SANS_ACCES, roleEnregistre);

        Projet nouveauProjet = new Projet();
        nouveauProjet.setTitre("Portail des marchés publics");
        nouveauProjet.setDateDebut(LocalDate.now());
        nouveauProjet.setDateFin(LocalDate.now().plusMonths(3));
        nouveauProjet.setPriorite(Priorite.NORMALE);
        nouveauProjet.setResponsable(chefDeProjet);
        projet = projetRepository.save(nouveauProjet);

        PartageClient partage = new PartageClient();
        partage.setClient(client);
        partage.setProjet(projet);
        partage.setStatut(StatutPartage.ACTIF);
        partage.setDateEcheance(LocalDate.now().plusMonths(1));
        partageClientRepository.save(partage);
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

    private PointInformation creerPoint(String titre) {
        PointInformation point = new PointInformation();
        point.setProjet(projet);
        point.setAuteur(chefDeProjet);
        point.setTitre(titre);
        point.setCorps("Corps de " + titre);
        return pointInformationRepository.save(point);
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor connecteEnTantQue(String matricule) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("preferred_username", matricule));
    }

    @Test
    void creer_chefDeProjet_publieLePointEtLeRetourne() throws Exception {
        PointInformationRequest requete = new PointInformationRequest();
        requete.setTitre("Authentification unique livrée");
        requete.setCorps("Les agents accèdent désormais au portail avec leur compte ministériel.");

        mockMvc.perform(post("/api/projets/{id}/points-information", projet.getId())
                        .with(connecteEnTantQue(MATRICULE_CHEF_PROJET))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Authentification unique livrée"))
                .andExpect(jsonPath("$.nomAuteur").value("Prenom Nom" + MATRICULE_CHEF_PROJET));

        org.junit.jupiter.api.Assertions.assertEquals(1, pointInformationRepository.count());
    }

    @Test
    void creer_clientAvecPartageActifMaisPasChefDeProjet_retourne403() throws Exception {
        PointInformationRequest requete = new PointInformationRequest();
        requete.setTitre("Titre");
        requete.setCorps("Corps");

        mockMvc.perform(post("/api/projets/{id}/points-information", projet.getId())
                        .with(connecteEnTantQue(MATRICULE_CLIENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(0, pointInformationRepository.count());
    }

    @Test
    void rechercher_chefDeProjet_retourneSesPointsDuPlusRecentAuPlusAncien() throws Exception {
        creerPoint("Ancien");
        Thread.sleep(20);
        creerPoint("Récent");

        mockMvc.perform(get("/api/projets/{id}/points-information", projet.getId()).with(connecteEnTantQue(MATRICULE_CHEF_PROJET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].titre").value("Récent"))
                .andExpect(jsonPath("$.content[1].titre").value("Ancien"));
    }

    @Test
    void rechercher_clientAvecPartageActif_retourneLesPoints() throws Exception {
        creerPoint("Environnement de démonstration ouvert");

        mockMvc.perform(get("/api/projets/{id}/points-information", projet.getId()).with(connecteEnTantQue(MATRICULE_CLIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void rechercher_utilisateurSansPartageNiResponsabilite_retourne403() throws Exception {
        creerPoint("Point");

        mockMvc.perform(get("/api/projets/{id}/points-information", projet.getId()).with(connecteEnTantQue(MATRICULE_SANS_ACCES)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechercher_clientAvecPartageRevoque_retourne403() throws Exception {
        partageClientRepository.findAll().forEach(p -> {
            p.setStatut(StatutPartage.REVOQUE);
            partageClientRepository.save(p);
        });
        creerPoint("Point");

        mockMvc.perform(get("/api/projets/{id}/points-information", projet.getId()).with(connecteEnTantQue(MATRICULE_CLIENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechercher_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/projets/{id}/points-information", projet.getId())).andExpect(status().isUnauthorized());
    }
}
