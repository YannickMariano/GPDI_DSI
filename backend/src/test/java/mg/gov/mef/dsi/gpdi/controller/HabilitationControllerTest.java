package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.HabilitationLigneRequest;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
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

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class HabilitationControllerTest {

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
    private HabilitationRepository habilitationRepository;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        habilitationRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("chef_projet");
        role.setDescription("Chef de projet");
        roleRepository.save(role);

        Habilitation habilitation = new Habilitation();
        habilitation.setModule("Module 2");
        habilitation.setFonction("Création des tâches");
        habilitation.setRoleNom("chef_projet");
        habilitation.setNiveau(NiveauAcces.LECTURE);
        habilitationRepository.save(habilitation);
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void rechercher_matriceExistante_retourneLesLignesRegroupeesParFonction() throws Exception {
        mockMvc.perform(get("/api/habilitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fonction").value("Création des tâches"))
                .andExpect(jsonPath("$[0].acces.chef_projet").value("lecture"));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void mettreAJour_caseValide_changeLeNiveauEtLePersiste() throws Exception {
        HabilitationLigneRequest ligne = new HabilitationLigneRequest();
        ligne.setFonction("Création des tâches");
        ligne.setRoleNom("chef_projet");
        ligne.setNiveau("ecriture");

        mockMvc.perform(put("/api/habilitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList(ligne))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acces.chef_projet").value("ecriture"));

        mockMvc.perform(get("/api/habilitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acces.chef_projet").value("ecriture"));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void mettreAJour_roleInconnu_retourne400() throws Exception {
        HabilitationLigneRequest ligne = new HabilitationLigneRequest();
        ligne.setFonction("Création des tâches");
        ligne.setRoleNom("role_fantome");
        ligne.setNiveau("ecriture");

        mockMvc.perform(put("/api/habilitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList(ligne))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechercher_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/habilitations"))
                .andExpect(status().isUnauthorized());
    }
}
