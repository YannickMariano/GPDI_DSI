package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.SousTacheRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.SousTacheRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SousTacheControllerTest {

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
    private SousTacheRepository sousTacheRepository;
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

    private Long idTache;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        sousTacheRepository.deleteAll();
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
        responsable.setMatricule("80001");
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail("resp80001@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleEnregistre);
        Utilisateur responsableEnregistre = utilisateurRepository.save(responsable);

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe SousTache");
        equipe.setChefEquipe(responsableEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        Projet projet = new Projet();
        projet.setTitre("Projet SousTache");
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(responsableEnregistre);
        projet.setEquipe(equipeEnregistree);
        Projet projetEnregistre = projetRepository.save(projet);

        Tache tache = new Tache();
        tache.setTitre("Tache Parente");
        tache.setPriorite(Priorite.NORMALE);
        tache.setDateFinPrevue(LocalDate.now().plusWeeks(2));
        tache.setProjet(projetEnregistre);
        idTache = tacheRepository.save(tache).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", "80001"))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    @Test
    void creer_tacheExistante_retourne201() throws Exception {
        SousTacheRequest requete = new SousTacheRequest();
        requete.setTitre("Ecrire les tests");

        mockMvc.perform(post("/api/taches/{idTache}/sous-taches", idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Ecrire les tests"))
                .andExpect(jsonPath("$.fait").value(false));
    }

    @Test
    void supprimer_sousTacheExistante_retourne204PuisListeVide() throws Exception {
        SousTacheRequest requete = new SousTacheRequest();
        requete.setTitre("A supprimer");

        String reponse = mockMvc.perform(post("/api/taches/{idTache}/sous-taches", idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(delete("/api/taches/{idTache}/sous-taches/{id}", idTache, id)
                .with(utilisateurConnecte()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/taches/{idTache}/sous-taches", idTache)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void basculer_sousTacheExistante_inverseFait() throws Exception {
        SousTacheRequest requete = new SousTacheRequest();
        requete.setTitre("A cocher");

        String reponse = mockMvc.perform(post("/api/taches/{idTache}/sous-taches", idTache)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(post("/api/taches/{idTache}/sous-taches/{id}/basculer", idTache, id)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fait").value(true));
    }
}
