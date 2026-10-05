package mg.gov.mef.dsi.gpdi.service.impl;

import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.SuggestionRisqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Suggestion de bout en bout : vrai contexte, validation, transaction et base PostgreSQL ; seul le
 * modele (Ollama) est simule.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SuggestionRisqueIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void proprietesBaseDeDonnees(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SuggestionRisqueService suggestionRisqueService;
    @Autowired
    private IaRecommandationRepository iaRecommandationRepository;
    @Autowired
    private IaAnalyseRepository iaAnalyseRepository;
    @Autowired
    private RisqueRepository risqueRepository;
    @Autowired
    private TacheRepository tacheRepository;
    @Autowired
    private MembreEquipeRepository membreEquipeRepository;
    @Autowired
    private ProjetRepository projetRepository;
    @Autowired
    private EquipeRepository equipeRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private OllamaClient ollamaClient;

    private Long idProjet;
    private Long idRisque;
    private Long idTache;
    private Long idResponsable;
    private Long idMembre;

    @BeforeEach
    void preparerFixtures() {
        iaRecommandationRepository.deleteAll();
        risqueRepository.deleteAll();
        iaAnalyseRepository.deleteAll();
        tacheRepository.deleteAll();
        membreEquipeRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("CHEF_DE_PROJET");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);
        Utilisateur responsable = utilisateurRepository.save(utilisateur("92001", roleEnregistre));
        Utilisateur membre = utilisateurRepository.save(utilisateur("92002", roleEnregistre));
        idResponsable = responsable.getId();
        idMembre = membre.getId();

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Suggestions");
        equipe.setChefEquipe(responsable);
        Equipe equipeEnregistree = equipeRepository.save(equipe);
        MembreEquipe membreEquipe = new MembreEquipe();
        membreEquipe.setEquipe(equipeEnregistree);
        membreEquipe.setUtilisateur(membre);
        membreEquipeRepository.save(membreEquipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Suggestions");
        projet.setStatut(StatutProjet.EN_COURS);
        projet.setDateDebut(LocalDate.now().minusDays(30));
        projet.setDateFin(LocalDate.now().plusDays(60));
        projet.setPriorite(Priorite.HAUTE);
        projet.setResponsable(responsable);
        projet.setEquipe(equipeEnregistree);
        Projet projetEnregistre = projetRepository.save(projet);
        idProjet = projetEnregistre.getId();

        Tache tache = new Tache();
        tache.setTitre("Migration des données");
        tache.setStatut(StatutTache.BLOQUEE);
        tache.setPriorite(Priorite.HAUTE);
        tache.setDateFinPrevue(LocalDate.now().minusDays(3));
        tache.setProjet(projetEnregistre);
        idTache = tacheRepository.save(tache).getId();

        Risque risque = new Risque();
        risque.setProjet(projetEnregistre);
        risque.setLibelle("Tâches bloquées et en retard");
        risque.setProbabilite(4);
        risque.setImpact(4);
        risque.setNiveau(NiveauRisque.CRITIQUE);
        risque.setStatut(StatutRisque.OUVERT);
        idRisque = risqueRepository.save(risque).getId();

        when(ollamaClient.getModele()).thenReturn("qwen3:8b");
    }

    private String reponseModele() {
        return "{\"solutions\":["
                + "{\"libelle\":\"Confier la migration à Prenom\",\"justification\":\"Elle est disponible\",\"gain\":\"3 jours de retard évités\","
                + "\"typeAction\":\"REASSIGNER_TACHE\",\"idTache\":" + idTache + ",\"idAssigne\":" + idMembre + "},"
                + "{\"libelle\":\"Réaffecter une tâche fantôme\",\"justification\":\"j\",\"gain\":\"g\","
                + "\"typeAction\":\"REASSIGNER_TACHE\",\"idTache\":99999,\"idAssigne\":" + idMembre + "},"
                + "{\"libelle\":\"Planifier une revue de déblocage\",\"justification\":\"Identifier les blocages\",\"gain\":\"1 jour\","
                + "\"typeAction\":\"CREER_TACHE\",\"titreTache\":\"Organiser la revue\",\"descriptionTache\":\"Revue hebdomadaire\","
                + "\"tempsEstime\":2.5,\"idAssigne\":" + idResponsable + "}]}";
    }

    @Test
    void suggerer_enregistreLesSolutionsPuisLesRenvoieDepuisLeCache() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn(reponseModele());

        List<SolutionRisqueResponse> premiere = suggestionRisqueService.suggerer(idProjet, idRisque, idResponsable);
        List<SolutionRisqueResponse> seconde = suggestionRisqueService.suggerer(idProjet, idRisque, idResponsable);

        assertThat(premiere).hasSize(3);
        assertThat(premiere).extracting(SolutionRisqueResponse::getTypeAction)
                .containsExactly("REASSIGNER_TACHE", "CREER_TACHE", "CREER_TACHE");
        assertThat(premiere.get(0).getGain()).isEqualTo("3 jours de retard évités");
        assertThat(seconde).extracting(SolutionRisqueResponse::getId)
                .containsExactlyElementsOf(premiere.stream().map(SolutionRisqueResponse::getId).collect(java.util.stream.Collectors.toList()));
        verify(ollamaClient, times(1)).genererJson(anyString(), anyString(), anyMap());

        List<IaAnalyse> analyses = iaAnalyseRepository.findAll();
        assertThat(analyses).hasSize(1);
        assertThat(analyses.get(0).getTypeAnalyse()).isEqualTo(TypeAnalyseIa.SUGGESTION_RISQUE);
        assertThat(analyses.get(0).getDemandeur().getId()).isEqualTo(idResponsable);
        assertThat(analyses.get(0).getQuestion()).contains("Tâches bloquées et en retard");
        assertThat(analyses.get(0).getContexte().get("taches").get("aSurveiller").get(0).get("id").asLong()).isEqualTo(idTache);

        List<IaRecommandation> recommandations = iaRecommandationRepository.findByRisqueIdOrderByIdAsc(idRisque);
        assertThat(recommandations).hasSize(3);
        assertThat(recommandations).allMatch(recommandation -> recommandation.getStatut() == StatutRecommandation.PROPOSEE);
        assertThat(recommandations.get(0).getActionProposee().get("idTache").asLong()).isEqualTo(idTache);
        assertThat(recommandations.get(0).getActionProposee().get("idAssigne").asLong()).isEqualTo(idMembre);
        assertThat(recommandations.get(1).getActionProposee().get("titre").asText()).isEqualTo("Réaffecter une tâche fantôme");
        assertThat(recommandations.get(2).getActionProposee().get("tempsEstime").asDouble()).isEqualTo(2.5);
        assertThat(recommandations.get(2).getActionProposee().get("idAssigne").asLong()).isEqualTo(idResponsable);
    }

    @Test
    void suggerer_risqueRejete_leveStatutRisqueInvalideSansInterrogerLeModele() {
        Risque risque = risqueRepository.findById(idRisque).get();
        risque.setStatut(StatutRisque.REJETE);
        risqueRepository.save(risque);

        assertThatThrownBy(() -> suggestionRisqueService.suggerer(idProjet, idRisque, idResponsable))
                .isInstanceOf(StatutRisqueInvalideException.class);

        verify(ollamaClient, times(0)).genererJson(anyString(), anyString(), anyMap());
    }

    @Test
    void suggerer_reponseInexploitable_neConserveRienEtPermetDeReessayer() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap()))
                .thenReturn("{\"solutions\":[]}")
                .thenReturn(reponseModele());

        assertThatThrownBy(() -> suggestionRisqueService.suggerer(idProjet, idRisque, idResponsable))
                .isInstanceOf(ReponseIaInvalideException.class);
        assertThat(iaAnalyseRepository.count()).isZero();
        assertThat(iaRecommandationRepository.count()).isZero();

        assertThat(suggestionRisqueService.suggerer(idProjet, idRisque, idResponsable)).hasSize(3);
    }

    private Utilisateur utilisateur(String matricule, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setMatricule(matricule);
        utilisateur.setNom("Nom" + matricule);
        utilisateur.setPrenom("Prenom");
        utilisateur.setEmail("u" + matricule + "@test.mg");
        utilisateur.setMotDePasse("HASH");
        utilisateur.setRole(role);
        return utilisateur;
    }
}
