package mg.gov.mef.dsi.gpdi.controller;

import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.response.RecommandationProjetResponse;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesRecommandationsProjetRefuseException;
import mg.gov.mef.dsi.gpdi.exception.AnalyseRecommandationsDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.RecommandationProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRecommandationProjetInvalideException;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.RecommandationProjetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
import java.util.Arrays;
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
class RecommandationProjetControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_PROJET";
    private static final String MATRICULE = "90020";
    private static final Long ID_PROJET = 7L;
    private static final Long ID_RECOMMANDATION = 42L;

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
    private RecommandationProjetService recommandationProjetService;

    private Long idUtilisateur;

    @BeforeEach
    void preparerUtilisateur() {
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("CHEF_DE_PROJET");
        role.setDescription("Chef de projet");
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setMatricule(MATRICULE);
        utilisateur.setNom("Nom");
        utilisateur.setPrenom("Prenom");
        utilisateur.setEmail("recommandation90020@test.mg");
        utilisateur.setMotDePasse("HASH");
        utilisateur.setRole(roleRepository.save(role));
        idUtilisateur = utilisateurRepository.save(utilisateur).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", MATRICULE))
                .authorities(new SimpleGrantedAuthority(role));
    }

    private RecommandationProjetResponse recommandation(String statut) {
        return new RecommandationProjetResponse(ID_RECOMMANDATION, "RETIRER_DU_SPRINT", "Retirer T-100 du sprint",
                "Sprint en surcapacité", "Sprint 4 : 41h/35h", statut, null, LocalDateTime.now());
    }

    @Test
    void lister_gestionnaireProjet_retourneLesRecommandations() throws Exception {
        when(recommandationProjetService.lister(ID_PROJET)).thenReturn(Arrays.asList(recommandation("PROPOSEE")));

        mockMvc.perform(get("/api/projets/{id}/recommandations", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].libelle").value("Retirer T-100 du sprint"));
    }

    @Test
    void lister_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/projets/{id}/recommandations", ID_PROJET)).andExpect(status().isUnauthorized());
        verifyNoInteractions(recommandationProjetService);
    }

    @Test
    void analyser_gestionnaireProjet_transmetLeDemandeurDuJeton() throws Exception {
        when(recommandationProjetService.analyser(ID_PROJET, idUtilisateur)).thenReturn(Arrays.asList(recommandation("PROPOSEE")));

        mockMvc.perform(post("/api/projets/{id}/recommandations/analyser", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(recommandationProjetService).analyser(ID_PROJET, idUtilisateur);
    }

    @Test
    void analyser_autreRole_retourne403SansAppelerLeService() throws Exception {
        mockMvc.perform(post("/api/projets/{id}/recommandations/analyser", ID_PROJET).with(utilisateurConnecte("ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recommandationProjetService);
    }

    @Test
    void analyser_utilisateurNonResponsableDuProjet_retourne403() throws Exception {
        when(recommandationProjetService.analyser(ID_PROJET, idUtilisateur)).thenThrow(new AccesRecommandationsProjetRefuseException(ID_PROJET));

        mockMvc.perform(post("/api/projets/{id}/recommandations/analyser", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void analyser_dejaEnCours_retourne409() throws Exception {
        when(recommandationProjetService.analyser(ID_PROJET, idUtilisateur)).thenThrow(new AnalyseRecommandationsDejaEnCoursException(ID_PROJET));

        mockMvc.perform(post("/api/projets/{id}/recommandations/analyser", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isConflict());
    }

    @Test
    void appliquer_gestionnaireProjet_retourneLaRecommandationAppliquee() throws Exception {
        RecommandationProjetResponse appliquee = new RecommandationProjetResponse(ID_RECOMMANDATION, "RETIRER_DU_SPRINT",
                "Retirer T-100 du sprint", "Sprint en surcapacité", "Sprint 4 : 41h/35h", "APPLIQUEE",
                "Tâche T-100 retirée du sprint.", LocalDateTime.now());
        when(recommandationProjetService.appliquer(ID_PROJET, ID_RECOMMANDATION, idUtilisateur)).thenReturn(appliquee);

        mockMvc.perform(post("/api/projets/{id}/recommandations/{idR}/appliquer", ID_PROJET, ID_RECOMMANDATION)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("APPLIQUEE"))
                .andExpect(jsonPath("$.resultat").value("Tâche T-100 retirée du sprint."));
    }

    @Test
    void appliquer_recommandationIntrouvable_retourne404() throws Exception {
        when(recommandationProjetService.appliquer(ID_PROJET, ID_RECOMMANDATION, idUtilisateur))
                .thenThrow(new RecommandationProjetIntrouvableException(ID_RECOMMANDATION, ID_PROJET));

        mockMvc.perform(post("/api/projets/{id}/recommandations/{idR}/appliquer", ID_PROJET, ID_RECOMMANDATION)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void appliquer_recommandationDejaTraitee_retourne409() throws Exception {
        when(recommandationProjetService.appliquer(ID_PROJET, ID_RECOMMANDATION, idUtilisateur))
                .thenThrow(new StatutRecommandationProjetInvalideException(ID_RECOMMANDATION, mg.gov.mef.dsi.gpdi.entity.StatutRecommandation.APPLIQUEE));

        mockMvc.perform(post("/api/projets/{id}/recommandations/{idR}/appliquer", ID_PROJET, ID_RECOMMANDATION)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isConflict());
    }

    @Test
    void rejeter_gestionnaireProjet_retourneLaRecommandationRejetee() throws Exception {
        when(recommandationProjetService.rejeter(ID_PROJET, ID_RECOMMANDATION, idUtilisateur)).thenReturn(recommandation("REJETEE"));

        mockMvc.perform(post("/api/projets/{id}/recommandations/{idR}/rejeter", ID_PROJET, ID_RECOMMANDATION)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETEE"));
    }

    @Test
    void rejeter_projetVide_neRenvoieRien() throws Exception {
        when(recommandationProjetService.lister(ID_PROJET)).thenReturn(Collections.<RecommandationProjetResponse>emptyList());

        mockMvc.perform(get("/api/projets/{id}/recommandations", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
