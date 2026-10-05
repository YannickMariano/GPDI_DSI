package mg.gov.mef.dsi.gpdi.service.impl;

import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.response.ResultatDetectionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.DetectionRisqueService;
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
import static org.mockito.Mockito.when;

/**
 * Detection de bout en bout : vrais contexte, validation, transaction et base PostgreSQL ; seul le
 * modele (Ollama) est simule.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DetectionRisqueIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void proprietesBaseDeDonnees(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DetectionRisqueService detectionRisqueService;
    @Autowired
    private RisqueRepository risqueRepository;
    @Autowired
    private IaAnalyseRepository iaAnalyseRepository;
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
    private Long idResponsable;
    private Long idMembre;
    private Long idRisqueSousControle;
    private Long idRisqueRejete;

    @BeforeEach
    void preparerFixtures() {
        risqueRepository.deleteAll();
        iaAnalyseRepository.deleteAll();
        membreEquipeRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("CHEF_DE_PROJET");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);
        Utilisateur responsable = utilisateurRepository.save(utilisateur("91001", roleEnregistre));
        Utilisateur membre = utilisateurRepository.save(utilisateur("91002", roleEnregistre));
        idResponsable = responsable.getId();
        idMembre = membre.getId();

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Risques");
        equipe.setChefEquipe(responsable);
        Equipe equipeEnregistree = equipeRepository.save(equipe);
        MembreEquipe membreEquipe = new MembreEquipe();
        membreEquipe.setEquipe(equipeEnregistree);
        membreEquipe.setUtilisateur(membre);
        membreEquipeRepository.save(membreEquipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Risques");
        projet.setStatut(StatutProjet.EN_COURS);
        projet.setDateDebut(LocalDate.now().minusDays(30));
        projet.setDateFin(LocalDate.now().plusDays(60));
        projet.setPriorite(Priorite.HAUTE);
        projet.setResponsable(responsable);
        projet.setEquipe(equipeEnregistree);
        Projet projetEnregistre = projetRepository.save(projet);
        idProjet = projetEnregistre.getId();

        idRisqueSousControle = risqueRepository.save(risque(projetEnregistre, "Retard de la migration des données",
                StatutRisque.SOUS_CONTROLE, 4, 4)).getId();
        idRisqueRejete = risqueRepository.save(risque(projetEnregistre, "Dépendance envers un prestataire unique",
                StatutRisque.REJETE, 3, 3)).getId();

        when(ollamaClient.getModele()).thenReturn("qwen3:8b");
    }

    @Test
    void detecter_reponseValide_enregistreLAnalyseLesNouveauxRisquesEtLesReevaluations() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn("{"
                + "\"nouveauxRisques\":["
                + "{\"libelle\":\"Surcharge de l'équipe\",\"description\":\"Trop d'heures restantes\",\"probabilite\":4,\"impact\":4,\"parade\":\"Redistribuer\",\"idPorteur\":" + idMembre + "},"
                + "{\"libelle\":\"Dépendance envers un prestataire unique\",\"description\":\"déjà rejeté\",\"probabilite\":3,\"impact\":3,\"parade\":\"x\"},"
                + "{\"libelle\":\"Jalon de recette en retard\",\"description\":\"desc\",\"probabilite\":2,\"impact\":2,\"parade\":\"Replanifier\",\"idPorteur\":99999}],"
                + "\"reevaluations\":[{\"idRisque\":" + idRisqueSousControle + ",\"probabilite\":1,\"impact\":2},"
                + "{\"idRisque\":" + idRisqueRejete + ",\"probabilite\":1,\"impact\":1}],"
                + "\"synthese\":\"Le projet dérive.\",\"indiceConfiance\":0.7}");

        ResultatDetectionRisqueResponse resultat = detectionRisqueService.detecter(idProjet, idResponsable);

        assertThat(resultat.getNombreNouveauxRisques()).isEqualTo(2);
        assertThat(resultat.getNombreRisquesReevalues()).isEqualTo(1);
        assertThat(resultat.getNombreRisquesClos()).isEqualTo(1);

        List<IaAnalyse> analyses = iaAnalyseRepository.findAll();
        assertThat(analyses).hasSize(1);
        IaAnalyse analyse = analyses.get(0);
        assertThat(analyse.getTypeAnalyse()).isEqualTo(TypeAnalyseIa.DETECTION_RISQUE);
        assertThat(analyse.getDemandeur().getId()).isEqualTo(idResponsable);
        assertThat(analyse.getReponse()).isEqualTo("Le projet dérive.");
        assertThat(analyse.getModele()).isEqualTo("qwen3:8b");
        assertThat(analyse.getIndiceConfiance()).isEqualByComparingTo("0.7");
        assertThat(analyse.getContexte().get("projet").get("titre").asText()).isEqualTo("Projet Risques");

        List<Risque> risques = risqueRepository.findByProjetIdOrderByIdAsc(idProjet);
        assertThat(risques).hasSize(4);
        Risque clos = risques.get(0);
        assertThat(clos.getStatut()).isEqualTo(StatutRisque.CLOS);
        assertThat(clos.getNiveau()).isEqualTo(NiveauRisque.FAIBLE);
        assertThat(risques.get(1).getStatut()).isEqualTo(StatutRisque.REJETE);
        assertThat(risques.get(1).getProbabilite()).isEqualTo(3);

        Risque surcharge = risques.get(2);
        assertThat(surcharge.getLibelle()).isEqualTo("Surcharge de l'équipe");
        assertThat(surcharge.getNiveau()).isEqualTo(NiveauRisque.CRITIQUE);
        assertThat(surcharge.getStatut()).isEqualTo(StatutRisque.OUVERT);
        assertThat(surcharge.getPorteur().getId()).isEqualTo(idMembre);
        assertThat(surcharge.getAnalyse().getId()).isEqualTo(analyse.getId());
        assertThat(surcharge.getDateCreation()).isNotNull();

        Risque jalon = risques.get(3);
        assertThat(jalon.getNiveau()).isEqualTo(NiveauRisque.FAIBLE);
        assertThat(jalon.getPorteur().getId()).isEqualTo(idResponsable);
    }

    @Test
    void detecter_analyseAutomatique_enregistreSansDemandeur() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap()))
                .thenReturn("{\"nouveauxRisques\":[],\"reevaluations\":[],\"synthese\":\"RAS\"}");

        detectionRisqueService.detecter(idProjet, null);

        List<IaAnalyse> analyses = iaAnalyseRepository.findAll();
        assertThat(analyses).hasSize(1);
        assertThat(analyses.get(0).getDemandeur()).isNull();
        assertThat(analyses.get(0).getIndiceConfiance()).isNull();
    }

    @Test
    void detecter_reponseInexploitable_neModifieRien() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn("pas du json");

        assertThatThrownBy(() -> detectionRisqueService.detecter(idProjet, null))
                .isInstanceOf(ReponseIaInvalideException.class);

        assertThat(iaAnalyseRepository.count()).isZero();
        assertThat(risqueRepository.findByProjetIdOrderByIdAsc(idProjet)).hasSize(2);
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

    private Risque risque(Projet projet, String libelle, StatutRisque statut, int probabilite, int impact) {
        Risque risque = new Risque();
        risque.setProjet(projet);
        risque.setLibelle(libelle);
        risque.setProbabilite(probabilite);
        risque.setImpact(impact);
        risque.setNiveau(NiveauRisque.calculer(probabilite, impact));
        risque.setStatut(statut);
        return risque;
    }
}
