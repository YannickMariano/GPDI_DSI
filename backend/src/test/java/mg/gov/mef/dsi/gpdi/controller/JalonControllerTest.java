package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.JalonRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.PartageClient;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
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
class JalonControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_JALON";

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

    @Autowired
    private PartageClientRepository partageClientRepository;

    private Long idProjet;
    private Long idAutreProjet;
    private String matriculeResponsable;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        partageClientRepository.deleteAll();
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

        idProjet = creerProjet("Projet Fixture", "50001");
        matriculeResponsable = "50001";
        idAutreProjet = creerProjet("Autre Projet", "50002");
    }

    private Long creerProjet(String titre, String matriculeResponsable) {
        Role role = new Role();
        role.setNom("chef_projet");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule(matriculeResponsable);
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail(matriculeResponsable + "@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleEnregistre);
        Utilisateur responsableEnregistre = utilisateurRepository.save(responsable);

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe " + titre);
        equipe.setChefEquipe(responsableEnregistre);
        Equipe equipeEnregistree = equipeRepository.save(equipe);

        Projet projet = new Projet();
        projet.setTitre(titre);
        projet.setDateDebut(LocalDate.now());
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(responsableEnregistre);
        projet.setEquipe(equipeEnregistree);
        // En cours : un jalon ne peut etre cloture que si le projet auquel il appartient l'est deja.
        projet.setStatut(StatutProjet.EN_COURS);
        return projetRepository.save(projet).getId();
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor utilisateurConnecte() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", matriculeResponsable))
                .authorities(new SimpleGrantedAuthority(ROLE_GESTIONNAIRE));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor connecteEnTantQue(String matricule) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("preferred_username", matricule));
    }

    /** Compte Direction métier avec un partage actif sur le projet fixture : doit pouvoir lire ses jalons. */
    private void creerClientAvecPartageActif(String matricule) {
        Role role = new Role();
        role.setNom("direction_metier");
        role.setDescription("Direction métier");
        Role roleEnregistre = roleRepository.save(role);

        Utilisateur client = new Utilisateur();
        client.setMatricule(matricule);
        client.setNom("Nom" + matricule);
        client.setPrenom("Prenom");
        client.setEmail(matricule + "@test.mg");
        client.setMotDePasse("HASH");
        client.setRole(roleEnregistre);
        Utilisateur clientEnregistre = utilisateurRepository.save(client);

        PartageClient partage = new PartageClient();
        partage.setClient(clientEnregistre);
        partage.setProjet(projetRepository.findById(idProjet).get());
        partage.setStatut(StatutPartage.ACTIF);
        partage.setDateEcheance(LocalDate.now().plusMonths(1));
        partageClientRepository.save(partage);
    }

    private JalonRequest nouvelleRequete(String titre) {
        JalonRequest requete = new JalonRequest();
        requete.setTitre(titre);
        requete.setDescription("Description " + titre);
        requete.setDateEcheance(LocalDate.now().plusMonths(1));
        return requete;
    }

    private Long creerJalonEtRecupererId(Long idProjetCible, String titre) throws Exception {
        String reponse = mockMvc.perform(post("/api/projets/{idProjet}/jalons", idProjetCible)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(titre))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }

    @Test
    void creer_projetExistant_retourne201AvecStatutAVenir() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/jalons", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Jalon 1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("A_VENIR"))
                .andExpect(jsonPath("$.idProjet").value(idProjet));
    }

    @Test
    void obtenir_jalonViaUnAutreProjet_retourne404() throws Exception {
        Long idJalon = creerJalonEtRecupererId(idProjet, "Jalon 2");

        mockMvc.perform(get("/api/projets/{idProjet}/jalons/{id}", idAutreProjet, idJalon)
                .with(utilisateurConnecte()))
                .andExpect(status().isNotFound());
    }

    @Test
    void modifier_jalonExistant_retourne200() throws Exception {
        Long idJalon = creerJalonEtRecupererId(idProjet, "Jalon 3");

        mockMvc.perform(put("/api/projets/{idProjet}/jalons/{id}", idProjet, idJalon)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Jalon 3 modifie"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Jalon 3 modifie"));
    }

    @Test
    void terminer_jalonAVenir_passeEnAtteint() throws Exception {
        Long idJalon = creerJalonEtRecupererId(idProjet, "Jalon 4");

        mockMvc.perform(post("/api/projets/{idProjet}/jalons/{id}/terminer", idProjet, idJalon)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ATTEINT"))
                .andExpect(jsonPath("$.dateValidation").value(LocalDate.now().toString()));
    }

    @Test
    void terminer_projetEnPreparation_retourne400() throws Exception {
        Long idJalon = creerJalonEtRecupererId(idProjet, "Jalon 4bis");
        Projet projet = projetRepository.findById(idProjet).get();
        projet.setStatut(StatutProjet.EN_PREPARATION);
        projetRepository.save(projet);

        mockMvc.perform(post("/api/projets/{idProjet}/jalons/{id}/terminer", idProjet, idJalon)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void annuler_jalonAVenir_passeEnAnnule() throws Exception {
        Long idJalon = creerJalonEtRecupererId(idProjet, "Jalon 5");

        mockMvc.perform(post("/api/projets/{idProjet}/jalons/{id}/annuler", idProjet, idJalon)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULER"));
    }

    @Test
    void terminer_jalonDejaAnnule_retourne400() throws Exception {
        Long idJalon = creerJalonEtRecupererId(idProjet, "Jalon 6");
        mockMvc.perform(post("/api/projets/{idProjet}/jalons/{id}/annuler", idProjet, idJalon)
                .with(utilisateurConnecte()));

        mockMvc.perform(post("/api/projets/{idProjet}/jalons/{id}/terminer", idProjet, idJalon)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/jalons", idProjet)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Jalon 7"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechercher_clientAvecPartageActif_retourneLesJalons() throws Exception {
        creerJalonEtRecupererId(idProjet, "Jalon 8");
        creerClientAvecPartageActif("50010");

        mockMvc.perform(get("/api/projets/{idProjet}/jalons", idProjet).with(connecteEnTantQue("50010")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void rechercher_utilisateurSansPartageNiResponsabilite_retourne403() throws Exception {
        Role role = new Role();
        role.setNom("membre_equipe");
        role.setDescription("Membre équipe");
        Role roleEnregistre = roleRepository.save(role);
        Utilisateur sansAcces = new Utilisateur();
        sansAcces.setMatricule("50011");
        sansAcces.setNom("Nom50011");
        sansAcces.setPrenom("Prenom");
        sansAcces.setEmail("50011@test.mg");
        sansAcces.setMotDePasse("HASH");
        sansAcces.setRole(roleEnregistre);
        utilisateurRepository.save(sansAcces);

        mockMvc.perform(get("/api/projets/{idProjet}/jalons", idProjet).with(connecteEnTantQue("50011")))
                .andExpect(status().isForbidden());
    }
}
