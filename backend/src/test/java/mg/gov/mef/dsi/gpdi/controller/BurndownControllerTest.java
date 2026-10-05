package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class BurndownControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_SPRINT";

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
    private SprintRepository sprintRepository;
    @Autowired
    private TacheRepository tacheRepository;
    @Autowired
    private ProjetRepository projetRepository;
    @Autowired
    private EquipeRepository equipeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;

    private Long idProjet;
    private Long idSprint;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        tacheRepository.deleteAll();
        sprintRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("CHEF_DE_PROJET");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule("90001");
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail("resp90001@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleEnregistre);
        Utilisateur responsableEnregistre = utilisateurRepository.save(responsable);

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Burndown");
        equipe.setChefEquipe(responsableEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Burndown");
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(responsableEnregistre);
        projet.setEquipe(equipeEnregistree);
        idProjet = projetRepository.save(projet).getId();

        Sprint sprint = new Sprint();
        sprint.setNom("Sprint Burndown");
        sprint.setDateDebut(LocalDate.now());
        sprint.setDateFin(LocalDate.now().plusDays(4));
        sprint.setStatut(StatutSprint.EN_COURS);
        sprint.setProjet(projet);
        idSprint = sprintRepository.save(sprint).getId();

        Tache tache = new Tache();
        tache.setTitre("Tache du sprint");
        tache.setPriorite(Priorite.NORMALE);
        tache.setTempsEstime(20.0);
        tache.setTempsPasse(5.0);
        tache.setProjet(projet);
        tache.setSprint(sprint);
        tacheRepository.save(tache);
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void obtenir_sprintValide_retourneUnPointParJourAvecIdealDecroissant() throws Exception {
        mockMvc.perform(get("/api/projets/{idProjet}/sprints/{idSprint}/burndown", idProjet, idSprint))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$[0].ideal").value(20.0))
                .andExpect(jsonPath("$[4].ideal").value(0.0))
                .andExpect(jsonPath("$[0].restant").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void obtenir_sprintDunAutreProjet_retourne404() throws Exception {
        mockMvc.perform(get("/api/projets/{idProjet}/sprints/{idSprint}/burndown", 999999L, idSprint))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenir_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/projets/{idProjet}/sprints/{idSprint}/burndown", idProjet, idSprint))
                .andExpect(status().isUnauthorized());
    }
}
