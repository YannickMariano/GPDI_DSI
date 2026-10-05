package mg.gov.mef.dsi.gpdi.controller;

import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.response.ResultatDetectionRisqueResponse;
import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.service.RisqueService;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionTraiteeResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TraitementRisqueResponse;
import mg.gov.mef.dsi.gpdi.exception.SolutionRisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRecommandationInvalideException;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.service.TraitementRisqueService;
import org.springframework.http.MediaType;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AnalyseDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException;
import mg.gov.mef.dsi.gpdi.exception.SuggestionDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.DetectionRisqueService;
import mg.gov.mef.dsi.gpdi.service.SuggestionRisqueService;
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
class RisqueControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_RISQUE";
    private static final String MATRICULE = "90003";
    private static final Long ID_PROJET = 7L;
    private static final Long ID_RISQUE = 9L;

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
    private RisqueService risqueService;
    @MockBean
    private DetectionRisqueService detectionRisqueService;
    @MockBean
    private SuggestionRisqueService suggestionRisqueService;
    @MockBean
    private TraitementRisqueService traitementRisqueService;

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
        utilisateur.setEmail("risque90003@test.mg");
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
    void detecter_gestionnaireRisque_retourne200EtTransmetLeDemandeurDuJeton() throws Exception {
        when(detectionRisqueService.detecter(ID_PROJET, idUtilisateur))
                .thenReturn(new ResultatDetectionRisqueResponse(12L, 3, 2, 1, "Synthese"));

        mockMvc.perform(post("/api/projets/{id}/risques/detection", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAnalyse").value(12))
                .andExpect(jsonPath("$.nombreNouveauxRisques").value(3))
                .andExpect(jsonPath("$.nombreRisquesReevalues").value(2))
                .andExpect(jsonPath("$.nombreRisquesClos").value(1))
                .andExpect(jsonPath("$.synthese").value("Synthese"));

        verify(detectionRisqueService).detecter(ID_PROJET, idUtilisateur);
    }

    @Test
    void detecter_autreRole_retourne403SansAppelerLeService() throws Exception {
        mockMvc.perform(post("/api/projets/{id}/risques/detection", ID_PROJET)
                        .with(utilisateurConnecte("ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(detectionRisqueService);
    }

    @Test
    void detecter_projetInconnu_retourne404() throws Exception {
        when(detectionRisqueService.detecter(ID_PROJET, idUtilisateur)).thenThrow(new ProjetIntrouvableException(ID_PROJET));

        mockMvc.perform(post("/api/projets/{id}/risques/detection", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detecter_analyseDejaEnCours_retourne409() throws Exception {
        when(detectionRisqueService.detecter(ID_PROJET, idUtilisateur)).thenThrow(new AnalyseDejaEnCoursException(ID_PROJET));

        mockMvc.perform(post("/api/projets/{id}/risques/detection", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void detecter_assistantIndisponible_retourne503() throws Exception {
        when(detectionRisqueService.detecter(ID_PROJET, idUtilisateur))
                .thenThrow(new OllamaIndisponibleException("L'assistant IA est indisponible."));

        mockMvc.perform(post("/api/projets/{id}/risques/detection", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void detecter_reponseIaInexploitable_retourne502() throws Exception {
        when(detectionRisqueService.detecter(ID_PROJET, idUtilisateur))
                .thenThrow(new ReponseIaInvalideException("La réponse est vide."));

        mockMvc.perform(post("/api/projets/{id}/risques/detection", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isBadGateway());
    }

    @Test
    void suggerer_gestionnaireRisque_retourne200AvecLesSolutions() throws Exception {
        when(suggestionRisqueService.suggerer(ID_PROJET, ID_RISQUE, idUtilisateur)).thenReturn(Arrays.asList(
                new SolutionRisqueResponse(31L, "Confier T-101 à Lala", "Elle est disponible", "2 jours évités", "REASSIGNER_TACHE", "PROPOSEE", null, null),
                new SolutionRisqueResponse(32L, "Planifier une revue", null, null, "CREER_TACHE", "PROPOSEE", null, null)));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/suggestions", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(31))
                .andExpect(jsonPath("$[0].libelle").value("Confier T-101 à Lala"))
                .andExpect(jsonPath("$[0].gain").value("2 jours évités"))
                .andExpect(jsonPath("$[0].typeAction").value("REASSIGNER_TACHE"))
                .andExpect(jsonPath("$[1].typeAction").value("CREER_TACHE"));

        verify(suggestionRisqueService).suggerer(ID_PROJET, ID_RISQUE, idUtilisateur);
    }

    @Test
    void suggerer_autreRole_retourne403SansAppelerLeService() throws Exception {
        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/suggestions", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte("ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(suggestionRisqueService);
    }

    @Test
    void suggerer_risqueInconnu_retourne404() throws Exception {
        when(suggestionRisqueService.suggerer(ID_PROJET, ID_RISQUE, idUtilisateur)).thenThrow(new RisqueIntrouvableException(ID_RISQUE));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/suggestions", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void suggerer_risqueNonOuvert_retourne409() throws Exception {
        when(suggestionRisqueService.suggerer(ID_PROJET, ID_RISQUE, idUtilisateur))
                .thenThrow(new StatutRisqueInvalideException(ID_RISQUE, StatutRisque.REJETE, "recevoir de nouvelles solutions"));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/suggestions", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void suggerer_rechercheDejaEnCours_retourne409() throws Exception {
        when(suggestionRisqueService.suggerer(ID_PROJET, ID_RISQUE, idUtilisateur)).thenThrow(new SuggestionDejaEnCoursException(ID_RISQUE));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/suggestions", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isConflict());
    }

    @Test
    void suggerer_assistantIndisponible_retourne503() throws Exception {
        when(suggestionRisqueService.suggerer(ID_PROJET, ID_RISQUE, idUtilisateur))
                .thenThrow(new OllamaIndisponibleException("L'assistant IA est indisponible."));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/suggestions", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void appliquer_gestionnaireRisque_retourne200AvecLeResultatDeChaqueSolution() throws Exception {
        when(traitementRisqueService.appliquer(ID_PROJET, ID_RISQUE, Arrays.asList(31L, 32L), idUtilisateur))
                .thenReturn(new TraitementRisqueResponse(ID_RISQUE, "SOUS_CONTROLE", Arrays.asList(
                        new SolutionTraiteeResponse(31L, "Confier T-101", "REASSIGNER_TACHE", 101L, "T-101", false, null),
                        new SolutionTraiteeResponse(32L, "Revue", "CREER_TACHE", 200L, "T-200", true, "Réaffectation impossible"))));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/appliquer", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsSolutions\":[31,32]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRisque").value(9))
                .andExpect(jsonPath("$.statut").value("SOUS_CONTROLE"))
                .andExpect(jsonPath("$.solutions.length()").value(2))
                .andExpect(jsonPath("$.solutions[0].codeTache").value("T-101"))
                .andExpect(jsonPath("$.solutions[0].suivi").value(false))
                .andExpect(jsonPath("$.solutions[1].suivi").value(true))
                .andExpect(jsonPath("$.solutions[1].remarque").value("Réaffectation impossible"));
    }

    @Test
    void appliquer_aucuneSolutionCochee_retourne400SansAppelerLeService() throws Exception {
        for (String corps : new String[]{"{\"idsSolutions\":[]}", "{}", "{\"idsSolutions\":[null]}"}) {
            mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/appliquer", ID_PROJET, ID_RISQUE)
                            .with(utilisateurConnecte(ROLE_GESTIONNAIRE))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corps))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(traitementRisqueService);
    }

    @Test
    void appliquer_autreRole_retourne403() throws Exception {
        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/appliquer", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte("ROLE_GESTIONNAIRE_TACHE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsSolutions\":[31]}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(traitementRisqueService);
    }

    @Test
    void appliquer_erreursMetier_retournent404Et409() throws Exception {
        when(traitementRisqueService.appliquer(ID_PROJET, ID_RISQUE, Arrays.asList(31L), idUtilisateur))
                .thenThrow(new SolutionRisqueIntrouvableException(31L, ID_RISQUE))
                .thenThrow(new StatutRecommandationInvalideException(31L, StatutRecommandation.APPLIQUEE))
                .thenThrow(new StatutRisqueInvalideException(ID_RISQUE, StatutRisque.SOUS_CONTROLE, "recevoir l'application de solutions"))
                .thenThrow(new RisqueIntrouvableException(ID_RISQUE));
        int[] attendus = {404, 409, 409, 404};

        for (int attendu : attendus) {
            mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/appliquer", ID_PROJET, ID_RISQUE)
                            .with(utilisateurConnecte(ROLE_GESTIONNAIRE))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idsSolutions\":[31]}"))
                    .andExpect(status().is(attendu))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Test
    void rejeter_avecMotif_retourne200EtTransmetLeMotif() throws Exception {
        when(traitementRisqueService.rejeter(ID_PROJET, ID_RISQUE, "Hors périmètre", idUtilisateur))
                .thenReturn(new TraitementRisqueResponse(ID_RISQUE, "REJETE", Collections.<SolutionTraiteeResponse>emptyList()));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/rejeter", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"Hors périmètre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETE"))
                .andExpect(jsonPath("$.solutions.length()").value(0));
    }

    @Test
    void rejeter_sansCorps_retourne200AvecMotifNull() throws Exception {
        when(traitementRisqueService.rejeter(ID_PROJET, ID_RISQUE, null, idUtilisateur))
                .thenReturn(new TraitementRisqueResponse(ID_RISQUE, "REJETE", Collections.<SolutionTraiteeResponse>emptyList()));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/rejeter", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETE"));

        verify(traitementRisqueService).rejeter(ID_PROJET, ID_RISQUE, null, idUtilisateur);
    }

    @Test
    void rejeter_risqueNonOuvert_retourne409() throws Exception {
        when(traitementRisqueService.rejeter(ID_PROJET, ID_RISQUE, null, idUtilisateur))
                .thenThrow(new StatutRisqueInvalideException(ID_RISQUE, StatutRisque.CLOS, "etre rejete"));

        mockMvc.perform(post("/api/projets/{id}/risques/{idRisque}/rejeter", ID_PROJET, ID_RISQUE)
                        .with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isConflict());
    }

    @Test
    void rechercher_gestionnaireRisque_retourne200AvecLaListeDesRisques() throws Exception {
        RisqueResponse risque = new RisqueResponse();
        risque.setId(10L);
        risque.setCode("R1");
        risque.setLibelle("Tâches bloquées");
        risque.setParade("Débloquer");
        risque.setAnalyse("Trois tâches bloquées");
        risque.setProbabilite(4);
        risque.setImpact(5);
        risque.setNiveau(NiveauRisque.CRITIQUE);
        risque.setStatut(StatutRisque.OUVERT);
        risque.setIdPorteur(7L);
        risque.setPorteur("Lala Rabe");
        risque.setSolutions(Arrays.asList(new SolutionRisqueResponse(31L, "Confier T-1", "Elle est disponible", "2 jours", "REASSIGNER_TACHE", "PROPOSEE", null, null)));
        when(risqueService.rechercher(ID_PROJET)).thenReturn(Arrays.asList(risque));

        mockMvc.perform(get("/api/projets/{id}/risques", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("R1"))
                .andExpect(jsonPath("$[0].parade").value("Débloquer"))
                .andExpect(jsonPath("$[0].analyse").value("Trois tâches bloquées"))
                .andExpect(jsonPath("$[0].niveau").value("CRITIQUE"))
                .andExpect(jsonPath("$[0].statut").value("OUVERT"))
                .andExpect(jsonPath("$[0].porteur").value("Lala Rabe"))
                .andExpect(jsonPath("$[0].solutions[0].id").value(31))
                .andExpect(jsonPath("$[0].solutions[0].gain").value("2 jours"));
    }

    @Test
    void rechercher_autreRole_retourne403() throws Exception {
        mockMvc.perform(get("/api/projets/{id}/risques", ID_PROJET).with(utilisateurConnecte("ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(risqueService);
    }

    @Test
    void rechercher_projetInconnu_retourne404() throws Exception {
        when(risqueService.rechercher(ID_PROJET)).thenThrow(new ProjetIntrouvableException(ID_PROJET));

        mockMvc.perform(get("/api/projets/{id}/risques", ID_PROJET).with(utilisateurConnecte(ROLE_GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }
}
