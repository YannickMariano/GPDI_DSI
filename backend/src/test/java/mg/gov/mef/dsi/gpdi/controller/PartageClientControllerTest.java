package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.PartageClientRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
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
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PartageClientControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_PARTAGE";

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
    private PartageClientRepository partageClientRepository;
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

    private Long idProjet;
    private Long idClient;
    private Long idNonClient;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        partageClientRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();
        habilitationRepository.deleteAll();

        Habilitation habilitation = new Habilitation();
        habilitation.setModule("Module 1");
        habilitation.setFonction("Partage de l'avancement au client");
        habilitation.setRoleNom("chef_projet");
        habilitation.setNiveau(NiveauAcces.ECRITURE);
        habilitationRepository.save(habilitation);

        Role roleChefProjet = new Role();
        roleChefProjet.setNom("chef_projet");
        roleChefProjet.setDescription("Chef de projet");
        Role roleChefProjetEnregistre = roleRepository.save(roleChefProjet);

        Role roleClient = new Role();
        roleClient.setNom("direction_metier");
        roleClient.setDescription("Client");
        Role roleClientEnregistre = roleRepository.save(roleClient);

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule("99001");
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail("resp99001@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleChefProjetEnregistre);
        Utilisateur responsableEnregistre = utilisateurRepository.save(responsable);

        Utilisateur client = new Utilisateur();
        client.setMatricule("99002");
        client.setNom("Client");
        client.setPrenom("Compte");
        client.setEmail("client99002@test.mg");
        client.setMotDePasse("HASH");
        client.setRole(roleClientEnregistre);
        idClient = utilisateurRepository.save(client).getId();

        Utilisateur nonClient = new Utilisateur();
        nonClient.setMatricule("99003");
        nonClient.setNom("PasClient");
        nonClient.setPrenom("Compte");
        nonClient.setEmail("nonclient99003@test.mg");
        nonClient.setMotDePasse("HASH");
        nonClient.setRole(roleChefProjetEnregistre);
        idNonClient = utilisateurRepository.save(nonClient).getId();

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Partage");
        equipe.setChefEquipe(responsableEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Partage");
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(responsableEnregistre);
        projet.setEquipe(equipeEnregistree);
        idProjet = projetRepository.save(projet).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", "99001"))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    private PartageClientRequest nouvelleRequete(Long idUtilisateur) {
        PartageClientRequest requete = new PartageClientRequest();
        requete.setIdUtilisateur(idUtilisateur);
        requete.setIdsProjets(Collections.singletonList(idProjet));
        requete.setDateEcheance(LocalDate.now().plusMonths(1));
        return requete;
    }

    @Test
    void creer_clientEligible_retourne201AvecStatutActif() throws Exception {
        mockMvc.perform(post("/api/partages-clients")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idClient))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].statut").value("ACTIF"))
                .andExpect(jsonPath("$[0].idProjet").value(idProjet));
    }

    @Test
    void creer_utilisateurSansRoleClient_retourne409() throws Exception {
        mockMvc.perform(post("/api/partages-clients")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idNonClient))))
                .andExpect(status().isConflict());
    }

    @Test
    void creer_partageDejaActifPourLeMemeProjetEtClient_retourne409() throws Exception {
        mockMvc.perform(post("/api/partages-clients")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idClient))));

        mockMvc.perform(post("/api/partages-clients")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idClient))))
                .andExpect(status().isConflict());
    }

    @Test
    void revoquer_partageActif_passeEnRevoqueEtPermetUnNouveauPartage() throws Exception {
        String reponse = mockMvc.perform(post("/api/partages-clients")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idClient))))
                .andReturn().getResponse().getContentAsString();
        Long idPartage = objectMapper.readTree(reponse).get(0).get("id").asLong();

        mockMvc.perform(post("/api/partages-clients/{id}/revoquer", idPartage)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REVOQUE"));

        mockMvc.perform(post("/api/partages-clients")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idClient))))
                .andExpect(status().isCreated());
    }

    @Test
    void revoquer_partageDejaRevoque_retourne400() throws Exception {
        String reponse = mockMvc.perform(post("/api/partages-clients")
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idClient))))
                .andReturn().getResponse().getContentAsString();
        Long idPartage = objectMapper.readTree(reponse).get(0).get("id").asLong();

        mockMvc.perform(post("/api/partages-clients/{id}/revoquer", idPartage)
                .with(utilisateurConnecte()));

        mockMvc.perform(post("/api/partages-clients/{id}/revoquer", idPartage)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/partages-clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(idClient))))
                .andExpect(status().isUnauthorized());
    }
}
