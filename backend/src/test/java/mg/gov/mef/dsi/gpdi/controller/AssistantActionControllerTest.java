package mg.gov.mef.dsi.gpdi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.entity.ActionAssistant;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutActionAssistant;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ActionAssistantImpossibleException;
import mg.gov.mef.dsi.gpdi.repository.ActionAssistantRepository;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.ActionAssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AssistantActionControllerTest {

    private static final String MATRICULE_CHEF = "71001";
    private static final String MATRICULE_AUTRE_CHEF = "71004";

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
    private ActionAssistantService actionAssistantService;
    @Autowired
    private ActionAssistantRepository actionAssistantRepository;
    @Autowired
    private IaAnalyseRepository iaAnalyseRepository;
    @Autowired
    private TacheRepository tacheRepository;
    @Autowired
    private AffectationTacheRepository affectationTacheRepository;
    @Autowired
    private SprintRepository sprintRepository;
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

    private Utilisateur chef;
    private Utilisateur membre;
    private Long idProjet;

    @BeforeEach
    void preparerFixtures() {
        actionAssistantRepository.deleteAll();
        iaAnalyseRepository.deleteAll();
        affectationTacheRepository.deleteAll();
        tacheRepository.deleteAll();
        sprintRepository.deleteAll();
        membreEquipeRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();
        habilitationRepository.deleteAll();

        for (String fonction : new String[]{"Création des tâches", "Affectation des tâches", "Priorisation des tâches",
                "Changement de statut", "Enregistrement du temps passé", "Gestion des sprints"}) {
            Habilitation habilitation = new Habilitation();
            habilitation.setModule("Module");
            habilitation.setFonction(fonction);
            habilitation.setRoleNom("chef_projet");
            habilitation.setNiveau(NiveauAcces.ECRITURE);
            habilitationRepository.save(habilitation);
        }

        Role roleChef = role("chef_projet");
        Role roleMembre = role("membre_equipe");
        chef = utilisateur(MATRICULE_CHEF, "Rakoto", "Jean", roleChef);
        membre = utilisateur("71002", "Koto", "Andry", roleMembre);
        utilisateur(MATRICULE_AUTRE_CHEF, "Rabe", "Lova", roleChef);

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Portail");
        equipe.setChefEquipe(chef);
        Equipe equipeEnregistree = equipeRepository.save(equipe);
        MembreEquipe membreEquipe = new MembreEquipe();
        membreEquipe.setEquipe(equipeEnregistree);
        membreEquipe.setUtilisateur(membre);
        membreEquipeRepository.save(membreEquipe);

        Projet projet = new Projet();
        projet.setTitre("Refonte du portail");
        projet.setDateDebut(LocalDate.now().minusDays(10));
        projet.setDateFin(LocalDate.now().plusMonths(3));
        projet.setPriorite(Priorite.NORMALE);
        projet.setResponsable(chef);
        projet.setEquipe(equipeEnregistree);
        idProjet = projetRepository.save(projet).getId();
    }

    @Test
    void confirmer_creationDeTacheAvecAffectation_creeLaTacheAssigneeEtMarqueLActionAppliquee() throws Exception {
        Long idAction = proposer("{\"type\":\"CREER_TACHE\",\"titre\":\"Rédiger la recette\",\"priorite\":\"haute\",\"personnes\":[\"Koto\"]}");

        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idAction)
                        .with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET", "ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("APPLIQUEE"))
                .andExpect(jsonPath("$.idProjet").value(idProjet))
                .andExpect(jsonPath("$.resultat").exists());

        List<Tache> taches = tacheRepository.findByProjetId(idProjet, Pageable.unpaged()).getContent();
        assertThat(taches).hasSize(1);
        assertThat(taches.get(0).getTitre()).isEqualTo("Rédiger la recette");
        assertThat(taches.get(0).getPriorite()).isEqualTo(Priorite.HAUTE);
        assertThat(affectationTacheRepository.findByTacheId(taches.get(0).getId()))
                .extracting(affectation -> affectation.getUtilisateur().getId())
                .containsExactly(membre.getId());
    }

    @Test
    void confirmer_changementDeStatutPuisSuppression_appliqueChaqueActionSurLaTacheDesigneeParSonCode() throws Exception {
        Long idCreation = proposer("{\"type\":\"CREER_TACHE\",\"titre\":\"Tâche à faire évoluer\"}");
        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idCreation)
                .with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET", "ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isOk());
        Tache tache = tacheRepository.findByProjetId(idProjet, Pageable.unpaged()).getContent().get(0);

        Long idStatut = proposer("{\"type\":\"CHANGER_STATUT_TACHE\",\"tache\":\"" + tache.getCode().toLowerCase() + "\",\"statut\":\"en cours\"}");
        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idStatut)
                        .with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET", "ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isOk());
        assertThat(tacheRepository.findById(tache.getId()).get().getStatut()).isEqualTo(StatutTache.EN_COURS);

        Long idSuppression = proposer("{\"type\":\"SUPPRIMER_TACHE\",\"tache\":\"" + tache.getCode() + "\"}");
        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idSuppression)
                        .with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET", "ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isOk());
        assertThat(tacheRepository.findById(tache.getId())).isEmpty();
    }

    @Test
    void confirmer_creationDeSprint_creeLeSprintDansLeProjet() throws Exception {
        Long idAction = proposer("{\"type\":\"CREER_SPRINT\",\"dateDebut\":\"" + LocalDate.now() + "\",\"dateFin\":\""
                + LocalDate.now().plusDays(13) + "\",\"capaciteHeures\":60}");

        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idAction)
                        .with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET", "ROLE_GESTIONNAIRE_SPRINT")))
                .andExpect(status().isOk());

        assertThat(sprintRepository.findByProjetId(idProjet)).hasSize(1);
        assertThat(sprintRepository.findByProjetId(idProjet).get(0).getNom()).isEqualTo("Sprint 1");
        assertThat(sprintRepository.findByProjetId(idProjet).get(0).getCapaciteHeures()).isEqualTo(60);
    }

    @Test
    void confirmer_sansLeRoleKeycloakDeLAction_retourne403EtNeFaitRien() throws Exception {
        Long idAction = proposer("{\"type\":\"CREER_TACHE\",\"titre\":\"Interdite\"}");

        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idAction)
                        .with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET")))
                .andExpect(status().isForbidden());

        assertThat(tacheRepository.findByProjetId(idProjet, Pageable.unpaged()).getContent()).isEmpty();
        assertThat(actionAssistantRepository.findById(idAction).get().getStatut()).isEqualTo(StatutActionAssistant.PROPOSEE);
    }

    @Test
    void annuler_puisConfirmer_annuleSansRienModifierPuisRetourne409() throws Exception {
        Long idAction = proposer("{\"type\":\"CREER_TACHE\",\"titre\":\"Annulée\"}");

        mockMvc.perform(post("/api/assistant/actions/{id}/annuler", idAction).with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idAction)
                        .with(connecte(MATRICULE_CHEF, "ROLE_GESTIONNAIRE_PROJET", "ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isConflict());

        assertThat(tacheRepository.findByProjetId(idProjet, Pageable.unpaged()).getContent()).isEmpty();
    }

    @Test
    void confirmer_actionDUnAutreUtilisateur_retourne404() throws Exception {
        Long idAction = proposer("{\"type\":\"CREER_TACHE\",\"titre\":\"Pas à vous\"}");

        mockMvc.perform(post("/api/assistant/actions/{id}/confirmer", idAction)
                        .with(connecte(MATRICULE_AUTRE_CHEF, "ROLE_GESTIONNAIRE_PROJET", "ROLE_GESTIONNAIRE_TACHE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void preparer_personneHorsEquipeOuTacheInconnue_leveActionAssistantImpossibleException() {
        assertThatThrownBy(() -> actionAssistantService.preparer(json("{\"type\":\"CREER_TACHE\",\"titre\":\"X\",\"personnes\":[\"Inconnu\"]}"), chef))
                .isInstanceOf(ActionAssistantImpossibleException.class)
                .hasMessageContaining("Inconnu");
        assertThatThrownBy(() -> actionAssistantService.preparer(json("{\"type\":\"SUPPRIMER_TACHE\",\"tache\":\"T-99999\"}"), chef))
                .isInstanceOf(ActionAssistantImpossibleException.class);
        assertThat(actionAssistantService.preparer(json("{\"reponse\":\"sans action\"}").path("action"), chef)).isNull();
    }

    private Long proposer(String actionJson) {
        IaAnalyse analyse = new IaAnalyse();
        analyse.setDemandeur(chef);
        analyse.setTypeAnalyse(TypeAnalyseIa.ANALYSE_PROJET);
        analyse.setPortee(PorteeAnalyseIa.GLOBAL);
        analyse.setQuestion("question");
        analyse.setReponse("reponse");
        analyse.setModele("test");
        IaAnalyse enregistree = iaAnalyseRepository.save(analyse);

        ActionAssistant action = actionAssistantService.preparer(json(actionJson), chef);
        action.setAnalyse(enregistree);
        return actionAssistantRepository.save(action).getId();
    }

    private JsonNode json(String contenu) {
        try {
            return objectMapper.readTree(contenu);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor connecte(String matricule, String... roles) {
        SimpleGrantedAuthority[] autorites = new SimpleGrantedAuthority[roles.length];
        for (int i = 0; i < roles.length; i++) {
            autorites[i] = new SimpleGrantedAuthority(roles[i]);
        }
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", matricule))
                .authorities(autorites);
    }

    private Role role(String nom) {
        Role role = new Role();
        role.setNom(nom);
        role.setDescription(nom);
        return roleRepository.save(role);
    }

    private Utilisateur utilisateur(String matricule, String nom, String prenom, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setMatricule(matricule);
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(matricule + "@test.mg");
        utilisateur.setMotDePasse("HASH");
        utilisateur.setRole(role);
        return utilisateurRepository.save(utilisateur);
    }
}
