package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.RoleRequest;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
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
class RoleControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_ROLE";

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
    private RoleRepository roleRepository;

    @BeforeEach
    void nettoyerBase() {
        roleRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void creer_requeteValide_retourne201EtRoleCree() throws Exception {
        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequeteCreation("ADMIN", "Administrateur"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nom").value("ADMIN"));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void obtenir_roleExistant_retourneLeRole() throws Exception {
        Long id = creerRoleEtRecupererId("ADMIN", "Administrateur");

        mockMvc.perform(get("/api/roles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Administrateur"));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void obtenir_roleInexistant_retourne404() throws Exception {
        mockMvc.perform(get("/api/roles/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void modifier_roleExistant_retourneLeRoleModifie() throws Exception {
        Long id = creerRoleEtRecupererId("ADMIN", "Administrateur");

        RoleRequest modification = new RoleRequest();
        modification.setNom("GESTIONNAIRE");
        modification.setDescription("Gestionnaire");

        mockMvc.perform(put("/api/roles/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("GESTIONNAIRE"));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void supprimer_roleExistant_retourne204PuisRoleIntrouvable() throws Exception {
        Long id = creerRoleEtRecupererId("ADMIN", "Administrateur");

        mockMvc.perform(delete("/api/roles/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/roles/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequeteCreation("ADMIN", "Administrateur"))))
                .andExpect(status().isUnauthorized());
    }

    private RoleRequest nouvelleRequeteCreation(String nom, String description) {
        RoleRequest requete = new RoleRequest();
        requete.setNom(nom);
        requete.setDescription(description);
        return requete;
    }

    private Long creerRoleEtRecupererId(String nom, String description) throws Exception {
        String reponse = mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequeteCreation(nom, description))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }
}
