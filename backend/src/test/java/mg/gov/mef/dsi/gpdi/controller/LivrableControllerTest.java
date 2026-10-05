package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRejetRequest;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.Jalon;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.LivrableRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class LivrableControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_LIVRABLE";

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
    private LivrableRepository livrableRepository;

    @Autowired
    private JalonRepository jalonRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private HabilitationRepository habilitationRepository;

    private Long idJalon;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        livrableRepository.deleteAll();
        jalonRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();
        habilitationRepository.deleteAll();

        Habilitation habilitation = new Habilitation();
        habilitation.setModule("Module 1");
        habilitation.setFonction("Gestion des jalons et livrables");
        habilitation.setRoleNom("chef_projet");
        habilitation.setNiveau(NiveauAcces.ECRITURE);
        habilitationRepository.save(habilitation);

        Role role = new Role();
        role.setNom("chef_projet");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule("60001");
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail("resp60001@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleEnregistre);
        Utilisateur responsableEnregistre = utilisateurRepository.save(responsable);

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Livrable");
        equipe.setChefEquipe(responsableEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Livrable");
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(responsableEnregistre);
        projet.setEquipe(equipeEnregistree);
        Projet projetEnregistre = projetRepository.save(projet);

        Jalon jalon = new Jalon();
        jalon.setTitre("Jalon Livrable");
        jalon.setDateEcheance(LocalDate.now().plusWeeks(3));
        jalon.setProjet(projetEnregistre);
        idJalon = jalonRepository.save(jalon).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", "60001"))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    private LivrableRequest nouvelleRequete(String description, LocalDate dateLivraison) {
        LivrableRequest requete = new LivrableRequest();
        requete.setDescription(description);
        requete.setType("Document");
        requete.setDateLivraison(dateLivraison);
        return requete;
    }

    private Long creerLivrableEtRecupererId(String description) throws Exception {
        String reponse = mockMvc.perform(post("/api/jalons/{idJalon}/livrables", idJalon)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(description, LocalDate.now().plusWeeks(1)))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }

    @Test
    void creer_dateLivraisonValide_retourne201AvecStatutAttendu() throws Exception {
        mockMvc.perform(post("/api/jalons/{idJalon}/livrables", idJalon)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Livrable 1", LocalDate.now().plusWeeks(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("ATTENDU"))
                .andExpect(jsonPath("$.idJalon").value(idJalon));
    }

    @Test
    void creer_dateLivraisonApresEcheanceJalon_retourne400() throws Exception {
        mockMvc.perform(post("/api/jalons/{idJalon}/livrables", idJalon)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        nouvelleRequete("Livrable Hors Delai", LocalDate.now().plusMonths(6)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void livrer_livrableAttendu_passeEnLivre() throws Exception {
        Long idLivrable = creerLivrableEtRecupererId("Livrable 2");

        mockMvc.perform(post("/api/jalons/{idJalon}/livrables/{id}/livrer", idJalon, idLivrable)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("LIVRE"))
                .andExpect(jsonPath("$.dateValidation").value(LocalDate.now().toString()));
    }

    @Test
    void rejeter_livrableAttendu_passeEnRejeteAvecMotif() throws Exception {
        Long idLivrable = creerLivrableEtRecupererId("Livrable 3");

        LivrableRejetRequest rejet = new LivrableRejetRequest();
        rejet.setMotifRejet("Ne correspond pas au cahier des charges");

        mockMvc.perform(post("/api/jalons/{idJalon}/livrables/{id}/rejeter", idJalon, idLivrable)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejet)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETE"))
                .andExpect(jsonPath("$.motifRejet").value("Ne correspond pas au cahier des charges"))
                .andExpect(jsonPath("$.dateRejet").value(LocalDate.now().toString()));
    }

    @Test
    void livrer_livrableDejaLivre_retourne400() throws Exception {
        Long idLivrable = creerLivrableEtRecupererId("Livrable 4");
        mockMvc.perform(post("/api/jalons/{idJalon}/livrables/{id}/livrer", idJalon, idLivrable)
                .with(utilisateurConnecte()));

        mockMvc.perform(post("/api/jalons/{idJalon}/livrables/{id}/livrer", idJalon, idLivrable)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/jalons/{idJalon}/livrables", idJalon)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Livrable 5", LocalDate.now().plusWeeks(1)))))
                .andExpect(status().isUnauthorized());
    }
}
