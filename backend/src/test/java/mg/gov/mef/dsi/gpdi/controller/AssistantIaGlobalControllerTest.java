package mg.gov.mef.dsi.gpdi.controller;

import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.response.MessageAssistantResponse;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesAssistantIaGlobalRefuseException;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.AssistantIaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AssistantIaGlobalControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_PROJET";
    private static final String MATRICULE = "90020";

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
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private AssistantIaService assistantIaService;

    private Long idUtilisateur;

    @BeforeEach
    void preparerUtilisateur() {
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("chef_projet");
        role.setDescription("Chef de projet");
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setMatricule(MATRICULE);
        utilisateur.setNom("Nom");
        utilisateur.setPrenom("Prenom");
        utilisateur.setEmail("assistant90020@test.mg");
        utilisateur.setMotDePasse("HASH");
        utilisateur.setRole(roleRepository.save(role));
        idUtilisateur = utilisateurRepository.save(utilisateur).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", MATRICULE))
                .authorities(new SimpleGrantedAuthority(role));
    }

    @Test
    void poserQuestion_gestionnaireProjet_retourne200EtTransmetLeDemandeurDuJeton() throws Exception {
        when(assistantIaService.poserQuestionGlobale("Quels projets sont en retard ?", idUtilisateur))
                .thenReturn(new MessageAssistantResponse(1L, "Quels projets sont en retard ?", "Aucun projet en retard.", "2 projets en cours", LocalDateTime.now()));

        mockMvc.perform(post("/api/assistant/messages")
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Quels projets sont en retard ?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reponse").value("Aucun projet en retard."))
                .andExpect(jsonPath("$.fondement").value("2 projets en cours"));

        verify(assistantIaService).poserQuestionGlobale("Quels projets sont en retard ?", idUtilisateur);
    }

    @Test
    void poserQuestion_questionVide_retourne400SansAppelerLeService() throws Exception {
        mockMvc.perform(post("/api/assistant/messages")
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(assistantIaService);
    }

    @Test
    void poserQuestion_autreRoleKeycloak_retourne403SansAppelerLeService() throws Exception {
        mockMvc.perform(post("/api/assistant/messages")
                        .with(utilisateurConnecte("ROLE_GESTIONNAIRE_TACHE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Question\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(assistantIaService);
    }

    @Test
    void poserQuestion_utilisateurNonChefDeProjet_retourne403() throws Exception {
        when(assistantIaService.poserQuestionGlobale("Question", idUtilisateur))
                .thenThrow(new AccesAssistantIaGlobalRefuseException());

        mockMvc.perform(post("/api/assistant/messages")
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Question\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void historique_gestionnaireProjet_retourneSesEchangesGlobaux() throws Exception {
        when(assistantIaService.historiqueGlobal()).thenReturn(Collections.singletonList(
                new MessageAssistantResponse(1L, "Question globale", "Réponse globale", null, LocalDateTime.now())));

        mockMvc.perform(get("/api/assistant/messages").with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].question").value("Question globale"));
    }

    @Test
    void historique_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/assistant/messages")).andExpect(status().isUnauthorized());
        verifyNoInteractions(assistantIaService);
    }
}
