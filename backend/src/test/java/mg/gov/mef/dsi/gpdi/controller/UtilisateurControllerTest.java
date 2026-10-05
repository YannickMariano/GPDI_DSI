package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurCreationRequest;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurModificationRequest;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UtilisateurControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_UTILISATEUR";

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
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long idRoleExistant;

    @BeforeEach
    void nettoyerBaseEtCreerUnRole() {
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("ADMIN");
        role.setDescription("Administrateur");
        idRoleExistant = roleRepository.save(role).getId();
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void creer_requeteValide_retourne201EtUtilisateurCreeActifSansMotDePasse() throws Exception {
        mockMvc.perform(post("/api/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequeteCreation("10001", idRoleExistant))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.matricule").value("10001"))
                .andExpect(jsonPath("$.actif").value(true))
                .andExpect(jsonPath("$.nomRole").value("ADMIN"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void creer_roleInexistant_retourne404() throws Exception {
        mockMvc.perform(post("/api/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequeteCreation("10002", 999999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void obtenir_utilisateurInexistant_retourne404() throws Exception {
        mockMvc.perform(get("/api/utilisateurs/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void modifier_sansMotDePasse_retourne200EtConserveLeCompte() throws Exception {
        Long id = creerUtilisateurEtRecupererId("10003", idRoleExistant);

        UtilisateurModificationRequest modification = new UtilisateurModificationRequest();
        modification.setMatricule("10003");
        modification.setNom("Rakoto");
        modification.setPrenom("Jean");
        modification.setEmail("jean.rakoto@test.mg");
        modification.setIdRole(idRoleExistant);

        mockMvc.perform(put("/api/utilisateurs/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Rakoto"));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void supprimer_utilisateurExistant_retourne204PuisIntrouvable() throws Exception {
        Long id = creerUtilisateurEtRecupererId("10004", idRoleExistant);

        mockMvc.perform(delete("/api/utilisateurs/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/utilisateurs/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequeteCreation("10005", idRoleExistant))))
                .andExpect(status().isUnauthorized());
    }

    private UtilisateurCreationRequest nouvelleRequeteCreation(String matricule, Long idRole) {
        UtilisateurCreationRequest requete = new UtilisateurCreationRequest();
        requete.setMatricule(matricule);
        requete.setNom("Nom");
        requete.setPrenom("Prenom");
        requete.setEmail(matricule + "@test.mg");
        requete.setMotDePasse("motdepasse123");
        requete.setIdRole(idRole);
        return requete;
    }

    private Long creerUtilisateurEtRecupererId(String matricule, Long idRole) throws Exception {
        String reponse = mockMvc.perform(post("/api/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequeteCreation(matricule, idRole))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }
}
