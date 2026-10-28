package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.ProjetRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

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
class ProjetControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_PROJET";

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
    private ProjetRepository projetRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private MembreEquipeRepository membreEquipeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private HabilitationRepository habilitationRepository;

    private Long idResponsableEligible;
    private Long idEquipe;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        projetRepository.deleteAll();
        membreEquipeRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();
        habilitationRepository.deleteAll();

        for (String fonction : new String[]{"Création des projets", "Clôture du projet"}) {
            Habilitation habilitation = new Habilitation();
            habilitation.setModule("Module 1");
            habilitation.setFonction(fonction);
            habilitation.setRoleNom("chef_projet");
            habilitation.setNiveau(NiveauAcces.ECRITURE);
            habilitationRepository.save(habilitation);
        }

        Role roleChefDeProjet = new Role();
        roleChefDeProjet.setNom("chef_projet");
        roleChefDeProjet.setDescription("Chef de projet");
        Long idRole = roleRepository.save(roleChefDeProjet).getId();

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule("30001");
        responsable.setNom("Rakoto");
        responsable.setPrenom("Jean");
        responsable.setEmail("jean.rakoto@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleRepository.findById(idRole).orElseThrow(
                () -> new IllegalStateException("Role de test introuvable")));
        idResponsableEligible = utilisateurRepository.save(responsable).getId();

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Test");
        equipe.setDescription("Equipe de test");
        equipe.setChefEquipe(utilisateurRepository.findById(idResponsableEligible).orElseThrow(
                () -> new IllegalStateException("Utilisateur de test introuvable")));
        idEquipe = equipeRepository.save(equipe).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", "30001"))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    private ProjetRequest nouvelleRequete(String titre) {
        ProjetRequest requete = new ProjetRequest();
        requete.setTitre(titre);
        requete.setDescription("Description " + titre);
        requete.setDateDebut(LocalDate.now());
        requete.setDateFin(LocalDate.now().plusMonths(6));
        requete.setPriorite(Priorite.NORMALE);
        requete.setIdResponsable(idResponsableEligible);
        requete.setIdEquipe(idEquipe);
        return requete;
    }

    private Long creerProjetEtRecupererId(String titre) throws Exception {
        String reponse = mockMvc.perform(post("/api/projets")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(titre))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }

    @Test
    void creer_requeteValide_retourne201EtLaisseLequipeInactive() throws Exception {
        mockMvc.perform(post("/api/projets")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Projet Alpha"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_PREPARATION"))
                .andExpect(jsonPath("$.progression").value(0))
                .andExpect(jsonPath("$.archive").value(false));

        Equipe equipe = equipeRepository.findById(idEquipe).orElseThrow(
                () -> new IllegalStateException("Equipe introuvable"));
        org.assertj.core.api.Assertions.assertThat(equipe.getActive()).isFalse();
    }

    @Test
    void creer_statutEnCoursDirect_activeLequipe() throws Exception {
        ProjetRequest requete = nouvelleRequete("Projet Zeta");
        requete.setStatut(mg.gov.mef.dsi.gpdi.entity.StatutProjet.EN_COURS);

        mockMvc.perform(post("/api/projets")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));

        Equipe equipe = equipeRepository.findById(idEquipe).orElseThrow(
                () -> new IllegalStateException("Equipe introuvable"));
        org.assertj.core.api.Assertions.assertThat(equipe.getActive()).isTrue();
    }

    @Test
    void obtenir_projetInexistant_retourne404() throws Exception {
        mockMvc.perform(get("/api/projets/{id}", 999999L)
                .with(utilisateurConnecte()))
                .andExpect(status().isNotFound());
    }

    @Test
    void modifier_statutClotureDirect_retourne400() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Beta");

        ProjetRequest modification = nouvelleRequete("Projet Beta");
        modification.setStatut(mg.gov.mef.dsi.gpdi.entity.StatutProjet.CLOTURE);

        mockMvc.perform(put("/api/projets/{id}", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifier_statutNonFourniDansLaRequete_neReinitialisePasLeStatutDuProjet() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Omicron");
        mockMvc.perform(post("/api/projets/{id}/demarrer", idProjet).with(utilisateurConnecte()));

        ProjetRequest modification = nouvelleRequete("Projet Omicron modifié");

        mockMvc.perform(put("/api/projets/{id}", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void cloturer_projetExistant_fixeStatutEtDateCloture() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Gamma");

        mockMvc.perform(post("/api/projets/{id}/cloturer", idProjet)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTURE"))
                .andExpect(jsonPath("$.dateCloture").value(LocalDate.now().toString()));
    }

    @Test
    void archiver_projetExistant_fixeStatutEtArchiveTrue() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Delta");

        mockMvc.perform(post("/api/projets/{id}/archiver", idProjet)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ARCHIVE"))
                .andExpect(jsonPath("$.archive").value(true));
    }

    @Test
    void demarrer_projetEnPreparation_passeEnCoursEtActiveLequipe() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Kappa");

        mockMvc.perform(post("/api/projets/{id}/demarrer", idProjet)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));

        Equipe equipe = equipeRepository.findById(idEquipe).orElseThrow(
                () -> new IllegalStateException("Equipe introuvable"));
        org.assertj.core.api.Assertions.assertThat(equipe.getActive()).isTrue();
    }

    @Test
    void demarrer_projetDejaEnCours_retourne400() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Lambda");
        mockMvc.perform(post("/api/projets/{id}/demarrer", idProjet).with(utilisateurConnecte()));

        mockMvc.perform(post("/api/projets/{id}/demarrer", idProjet)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void suspendre_projetExistant_fixeStatutSuspendu() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Eta");

        mockMvc.perform(post("/api/projets/{id}/suspendre", idProjet)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SUSPENDU"));
    }

    @Test
    void reprendre_projetSuspendu_passeEnCours() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Mu");
        mockMvc.perform(post("/api/projets/{id}/suspendre", idProjet).with(utilisateurConnecte()));

        mockMvc.perform(post("/api/projets/{id}/reprendre", idProjet)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void reprendre_projetNonSuspendu_retourne400() throws Exception {
        Long idProjet = creerProjetEtRecupererId("Projet Nu");

        mockMvc.perform(post("/api/projets/{id}/reprendre", idProjet)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creer_requeteValide_genereUnCodeReferenceUnique() throws Exception {
        String reponse = mockMvc.perform(post("/api/projets")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Projet Theta"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String codeReference = objectMapper.readTree(reponse).get("codeReference").asText();
        org.assertj.core.api.Assertions.assertThat(codeReference)
                .startsWith("GPDI-" + LocalDate.now().getYear() + "-");
    }

    @Test
    void creer_sansEquipe_retourne201SansEquipeAssignee() throws Exception {
        ProjetRequest requete = nouvelleRequete("Projet Iota");
        requete.setIdEquipe(null);

        mockMvc.perform(post("/api/projets")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEquipe").doesNotExist());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/projets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Projet Epsilon"))))
                .andExpect(status().isUnauthorized());
    }
}
