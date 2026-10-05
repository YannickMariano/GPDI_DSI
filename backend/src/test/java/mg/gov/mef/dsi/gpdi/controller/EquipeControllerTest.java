package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.EquipeRequest;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
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

import java.util.Collections;

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
class EquipeControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_EQUIPE";

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
    private EquipeRepository equipeRepository;

    @Autowired
    private MembreEquipeRepository membreEquipeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long idChefEligible;
    private Long idMembreEligible;
    private Long idUtilisateurNonEligible;

    @BeforeEach
    void nettoyerBaseEtPreparerUtilisateurs() {
        membreEquipeRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Long idRoleMembreEquipe = creerRole("membre_equipe");
        Long idRoleAdmin = creerRole("administrateur");

        idChefEligible = creerUtilisateur("20001", idRoleMembreEquipe);
        idMembreEligible = creerUtilisateur("20002", idRoleMembreEquipe);
        idUtilisateurNonEligible = creerUtilisateur("20003", idRoleAdmin);
    }

    private Long creerRole(String nom) {
        Role role = new Role();
        role.setNom(nom);
        role.setDescription(nom);
        return roleRepository.save(role).getId();
    }

    private Long creerUtilisateur(String matricule, Long idRole) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setMatricule(matricule);
        utilisateur.setNom("Nom" + matricule);
        utilisateur.setPrenom("Prenom" + matricule);
        utilisateur.setEmail(matricule + "@test.mg");
        utilisateur.setMotDePasse("HASH");
        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new IllegalStateException("Role de test introuvable : " + idRole));
        utilisateur.setRole(role);
        return utilisateurRepository.save(utilisateur).getId();
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void creer_chefEtMembreEligibles_retourne201() throws Exception {
        EquipeRequest requete = nouvelleRequete("Equipe Alpha", idChefEligible, Collections.singletonList(idMembreEligible));

        mockMvc.perform(post("/api/equipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.membres", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void creer_chefNonEligible_retourne409() throws Exception {
        EquipeRequest requete = nouvelleRequete("Equipe Beta", idUtilisateurNonEligible, Collections.singletonList(idMembreEligible));

        mockMvc.perform(post("/api/equipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void obtenir_equipeInexistante_retourne404() throws Exception {
        mockMvc.perform(get("/api/equipes/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void modifier_retireLeMembreExistant_neLaisseAucunMembre() throws Exception {
        Long idEquipe = creerEquipeEtRecupererId("Equipe Gamma", idChefEligible, Collections.singletonList(idMembreEligible));

        EquipeRequest modification = nouvelleRequete("Equipe Gamma", idChefEligible, Collections.singletonList(idChefEligible));

        mockMvc.perform(put("/api/equipes/{id}", idEquipe)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membres", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.membres[0].idUtilisateur").value(idChefEligible));
    }

    @Test
    @WithMockUser(authorities = ROLE_GESTIONNAIRE)
    void supprimer_equipeExistante_retourne204PuisIntrouvable() throws Exception {
        Long idEquipe = creerEquipeEtRecupererId("Equipe Delta", idChefEligible, Collections.singletonList(idMembreEligible));

        mockMvc.perform(delete("/api/equipes/{id}", idEquipe))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/equipes/{id}", idEquipe))
                .andExpect(status().isNotFound());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        EquipeRequest requete = nouvelleRequete("Equipe Epsilon", idChefEligible, Collections.singletonList(idMembreEligible));

        mockMvc.perform(post("/api/equipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isUnauthorized());
    }

    private EquipeRequest nouvelleRequete(String nom, Long idChef, java.util.List<Long> idsMembres) {
        EquipeRequest requete = new EquipeRequest();
        requete.setNom(nom);
        requete.setDescription("Description " + nom);
        requete.setIdChefEquipe(idChef);
        requete.setIdsMembres(idsMembres);
        return requete;
    }

    private Long creerEquipeEtRecupererId(String nom, Long idChef, java.util.List<Long> idsMembres) throws Exception {
        String reponse = mockMvc.perform(post("/api/equipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(nom, idChef, idsMembres))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }
}
