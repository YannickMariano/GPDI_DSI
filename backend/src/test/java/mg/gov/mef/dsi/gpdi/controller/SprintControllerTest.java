package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.SprintRequest;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.PartageClient;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SprintControllerTest {

    private static final String ROLE_GESTIONNAIRE = "ROLE_GESTIONNAIRE_SPRINT";

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
    private SprintRepository sprintRepository;

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
        sprintRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();
        habilitationRepository.deleteAll();

        habilitationRepository.save(habilitation("Gestion des sprints"));
        habilitationRepository.save(habilitation("Clôture du sprint"));

        idProjet = creerProjet("Projet Fixture", "40001");
        matriculeResponsable = "40001";
        idAutreProjet = creerProjet("Autre Projet", "40002");
    }

    private Habilitation habilitation(String fonction) {
        Habilitation habilitation = new Habilitation();
        habilitation.setModule("Module 1");
        habilitation.setFonction(fonction);
        habilitation.setRoleNom("chef_projet");
        habilitation.setNiveau(NiveauAcces.ECRITURE);
        return habilitation;
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

    /** Compte Direction métier avec un partage actif sur le projet fixture : doit pouvoir lire ses sprints. */
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

    private SprintRequest nouvelleRequete(String nom) {
        SprintRequest requete = new SprintRequest();
        requete.setNom(nom);
        requete.setDateDebut(LocalDate.now());
        requete.setDateFin(LocalDate.now().plusWeeks(2));
        requete.setObjectif("Objectif " + nom);
        requete.setCapaciteHeures(40);
        return requete;
    }

    private Long creerSprintEtRecupererId(Long idProjetCible, String nom) throws Exception {
        String reponse = mockMvc.perform(post("/api/projets/{idProjet}/sprints", idProjetCible)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete(nom))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }

    @Test
    void creer_projetExistant_retourne201() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/sprints", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Sprint 1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.progression").value(0))
                .andExpect(jsonPath("$.idProjet").value(idProjet));
    }

    @Test
    void creer_projetInexistant_retourne404() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/sprints", 999999L)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Sprint X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenir_sprintDuBonProjet_retourneLeSprint() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint 2");

        mockMvc.perform(get("/api/projets/{idProjet}/sprints/{id}", idProjet, idSprint)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Sprint 2"));
    }

    @Test
    void obtenir_sprintViaUnAutreProjet_retourne404() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint 3");

        mockMvc.perform(get("/api/projets/{idProjet}/sprints/{id}", idAutreProjet, idSprint)
                .with(utilisateurConnecte()))
                .andExpect(status().isNotFound());
    }

    @Test
    void modifier_sprintExistant_retourne200() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint 4");

        SprintRequest modification = nouvelleRequete("Sprint 4 modifie");

        mockMvc.perform(put("/api/projets/{idProjet}/sprints/{id}", idProjet, idSprint)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Sprint 4 modifie"));
    }

    @Test
    void modifier_avecNouvelleCapacite_metAJourLaCapacite() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint Capacite");

        SprintRequest modification = nouvelleRequete("Sprint Capacite");
        modification.setCapaciteHeures(70);

        mockMvc.perform(put("/api/projets/{idProjet}/sprints/{id}", idProjet, idSprint)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaciteHeures").value(70));
    }

    @Test
    void creer_sansCapacite_retourne400() throws Exception {
        SprintRequest requete = nouvelleRequete("Sprint Sans Capacite");
        requete.setCapaciteHeures(null);

        mockMvc.perform(post("/api/projets/{idProjet}/sprints", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supprimer_sprintExistant_retourne204PuisIntrouvable() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint 5");

        mockMvc.perform(delete("/api/projets/{idProjet}/sprints/{id}", idProjet, idSprint)
                .with(utilisateurConnecte()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projets/{idProjet}/sprints/{id}", idProjet, idSprint)
                .with(utilisateurConnecte()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creer_sprint_estCreeAvecStatutPlanifie() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/sprints", idProjet)
                .with(utilisateurConnecte())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Sprint Statut"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("PLANIFIE"));
    }

    @Test
    void commencer_sprintPlanifie_passeEnCours() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint 7");

        mockMvc.perform(post("/api/projets/{idProjet}/sprints/{id}/commencer", idProjet, idSprint)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void terminer_sprintEnCours_passeEnCloture() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint 8");
        mockMvc.perform(post("/api/projets/{idProjet}/sprints/{id}/commencer", idProjet, idSprint)
                .with(utilisateurConnecte()));

        mockMvc.perform(post("/api/projets/{idProjet}/sprints/{id}/terminer", idProjet, idSprint)
                .with(utilisateurConnecte()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTURER"));
    }

    @Test
    void terminer_sprintEncorePlanifie_retourne400() throws Exception {
        Long idSprint = creerSprintEtRecupererId(idProjet, "Sprint 9");

        mockMvc.perform(post("/api/projets/{idProjet}/sprints/{id}/terminer", idProjet, idSprint)
                .with(utilisateurConnecte()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creer_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/projets/{idProjet}/sprints", idProjet)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nouvelleRequete("Sprint 6"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechercher_clientAvecPartageActif_retourneLesSprints() throws Exception {
        creerSprintEtRecupererId(idProjet, "Sprint 10");
        creerClientAvecPartageActif("40010");

        mockMvc.perform(get("/api/projets/{idProjet}/sprints", idProjet).with(connecteEnTantQue("40010")))
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
        sansAcces.setMatricule("40011");
        sansAcces.setNom("Nom40011");
        sansAcces.setPrenom("Prenom");
        sansAcces.setEmail("40011@test.mg");
        sansAcces.setMotDePasse("HASH");
        sansAcces.setRole(roleEnregistre);
        utilisateurRepository.save(sansAcces);

        mockMvc.perform(get("/api/projets/{idProjet}/sprints", idProjet).with(connecteEnTantQue("40011")))
                .andExpect(status().isForbidden());
    }
}
