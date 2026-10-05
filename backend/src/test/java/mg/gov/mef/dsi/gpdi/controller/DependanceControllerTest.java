package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.DependanceRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeLien;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.TachePrecedenceRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DependanceControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_TACHE";

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
    private TachePrecedenceRepository tachePrecedenceRepository;
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

    private Long idTacheA;
    private Long idTacheB;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        tachePrecedenceRepository.deleteAll();
        tacheRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("CHEF_DE_PROJET");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule("95001");
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail("resp95001@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleEnregistre);
        Utilisateur responsableEnregistre = utilisateurRepository.save(responsable);

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Dependance");
        equipe.setChefEquipe(responsableEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Dependance");
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(responsableEnregistre);
        projet.setEquipe(equipeEnregistree);
        Projet projetEnregistre = projetRepository.save(projet);

        Tache tacheA = new Tache();
        tacheA.setTitre("Tache A");
        tacheA.setPriorite(Priorite.NORMALE);
        tacheA.setDateFinPrevue(LocalDate.now().plusWeeks(2));
        tacheA.setProjet(projetEnregistre);
        idTacheA = tacheRepository.save(tacheA).getId();

        Tache tacheB = new Tache();
        tacheB.setTitre("Tache B");
        tacheB.setPriorite(Priorite.NORMALE);
        tacheB.setDateFinPrevue(LocalDate.now().plusWeeks(2));
        tacheB.setProjet(projetEnregistre);
        idTacheB = tacheRepository.save(tacheB).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", "95001"))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    @Test
    void creer_deuxTachesDistinctes_retourne201() throws Exception {
        DependanceRequest requete = new DependanceRequest();
        requete.setIdTacheCible(idTacheB);
        requete.setTypeLien(TypeLien.FIN_DEBUT);

        mockMvc.perform(post("/api/taches/{idTache}/dependances", idTacheA)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTacheSource").value(idTacheA))
                .andExpect(jsonPath("$.idTacheCible").value(idTacheB))
                .andExpect(jsonPath("$.typeLien").value("FIN_DEBUT"));
    }

    @Test
    void creer_relationDejaExistante_retourne409() throws Exception {
        DependanceRequest requete = new DependanceRequest();
        requete.setIdTacheCible(idTacheB);
        requete.setTypeLien(TypeLien.FIN_DEBUT);
        mockMvc.perform(post("/api/taches/{idTache}/dependances", idTacheA)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)));

        mockMvc.perform(post("/api/taches/{idTache}/dependances", idTacheA)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isConflict());
    }

    @Test
    void creer_tacheDependDelleMeme_retourne400() throws Exception {
        DependanceRequest requete = new DependanceRequest();
        requete.setIdTacheCible(idTacheA);
        requete.setTypeLien(TypeLien.FIN_DEBUT);

        mockMvc.perform(post("/api/taches/{idTache}/dependances", idTacheA)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supprimer_dependanceExistante_retourne204() throws Exception {
        DependanceRequest requete = new DependanceRequest();
        requete.setIdTacheCible(idTacheB);
        requete.setTypeLien(TypeLien.DEBUT_DEBUT);
        mockMvc.perform(post("/api/taches/{idTache}/dependances", idTacheA)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)));

        mockMvc.perform(delete("/api/taches/{idTache}/dependances/{idAutreTache}", idTacheA, idTacheB)
                .with(utilisateurConnecte()))
                .andExpect(status().isNoContent());
    }
}
