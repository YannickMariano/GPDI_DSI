package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.RisqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

/** Liste des risques contre un vrai PostgreSQL : requete sur les solutions en attente, mapping et numerotation. */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class RisqueServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void proprietesBaseDeDonnees(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RisqueService risqueService;
    @Autowired
    private IaRecommandationRepository iaRecommandationRepository;
    @Autowired
    private IaAnalyseRepository iaAnalyseRepository;
    @Autowired
    private RisqueRepository risqueRepository;
    @Autowired
    private ProjetRepository projetRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private Long idProjet;
    private Long idProjetSansRisque;
    private Long idAutreProjet;
    private Long idPorteur;

    @BeforeEach
    void preparerFixtures() {
        iaRecommandationRepository.deleteAll();
        risqueRepository.deleteAll();
        iaAnalyseRepository.deleteAll();
        projetRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("CHEF_DE_PROJET");
        role.setDescription("Chef de projet");
        Utilisateur porteur = new Utilisateur();
        porteur.setMatricule("94001");
        porteur.setNom("Rabe");
        porteur.setPrenom("Lala");
        porteur.setEmail("u94001@test.mg");
        porteur.setMotDePasse("HASH");
        porteur.setRole(roleRepository.save(role));
        Utilisateur porteurEnregistre = utilisateurRepository.save(porteur);
        idPorteur = porteurEnregistre.getId();

        Projet projet = projet("Projet A", porteurEnregistre);
        idProjet = projet.getId();
        idProjetSansRisque = projet("Projet B", porteurEnregistre).getId();
        Projet autreProjet = projet("Projet C", porteurEnregistre);
        idAutreProjet = autreProjet.getId();

        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(projet);
        analyse.setTypeAnalyse(TypeAnalyseIa.SUGGESTION_RISQUE);
        IaAnalyse analyseEnregistree = iaAnalyseRepository.save(analyse);

        Risque r1 = risqueRepository.save(risque(projet, "Tâches bloquées", StatutRisque.OUVERT, porteurEnregistre));
        Risque r2 = risqueRepository.save(risque(projet, "Surcharge", StatutRisque.SOUS_CONTROLE, null));
        risqueRepository.save(risque(autreProjet, "Risque d'un autre projet", StatutRisque.OUVERT, null));

        solution(analyseEnregistree, r1, "Confier T-1", StatutRecommandation.PROPOSEE, "2 jours évités");
        solution(analyseEnregistree, r1, "Solution rejetée", StatutRecommandation.REJETEE, null);
        solution(analyseEnregistree, r2, "Solution appliquée", StatutRecommandation.APPLIQUEE, null);
    }

    @Test
    void rechercher_retourneLesRisquesDuProjetNumerotesAvecLeursSolutionsEnAttenteSeulement() {
        List<RisqueResponse> risques = risqueService.rechercher(idProjet);

        assertThat(risques).hasSize(2);
        RisqueResponse r1 = risques.get(0);
        assertThat(r1.getCode()).isEqualTo("R1");
        assertThat(r1.getLibelle()).isEqualTo("Tâches bloquées");
        assertThat(r1.getParade()).isEqualTo("Parade de Tâches bloquées");
        assertThat(r1.getAnalyse()).isEqualTo("Analyse de Tâches bloquées");
        assertThat(r1.getNiveau()).isEqualTo(NiveauRisque.CRITIQUE);
        assertThat(r1.getStatut()).isEqualTo(StatutRisque.OUVERT);
        assertThat(r1.getDateDetection()).isNotNull();
        assertThat(r1.getIdPorteur()).isEqualTo(idPorteur);
        assertThat(r1.getPorteur()).isEqualTo("Lala Rabe");
        assertThat(r1.getSolutions()).hasSize(2);
        assertThat(r1.getSolutions().get(0).getLibelle()).isEqualTo("Confier T-1");
        assertThat(r1.getSolutions().get(0).getGain()).isEqualTo("2 jours évités");
        assertThat(r1.getSolutions().get(1).getLibelle()).isEqualTo("Solution rejetée");

        RisqueResponse r2 = risques.get(1);
        assertThat(r2.getCode()).isEqualTo("R2");
        assertThat(r2.getStatut()).isEqualTo(StatutRisque.SOUS_CONTROLE);
        assertThat(r2.getPorteur()).isNull();
        assertThat(r2.getSolutions()).hasSize(1);
        assertThat(r2.getSolutions().get(0).getLibelle()).isEqualTo("Solution appliquée");
    }

    @Test
    void rechercher_numerotationIndependanteParProjet() {
        assertThat(risqueService.rechercher(idAutreProjet)).extracting(RisqueResponse::getCode).containsExactly("R1");
        assertThat(risqueService.rechercher(idProjetSansRisque)).isEmpty();
    }

    private Projet projet(String titre, Utilisateur responsable) {
        Projet projet = new Projet();
        projet.setTitre(titre);
        projet.setDateDebut(LocalDate.now().minusDays(30));
        projet.setDateFin(LocalDate.now().plusDays(60));
        projet.setPriorite(Priorite.HAUTE);
        projet.setResponsable(responsable);
        return projetRepository.save(projet);
    }

    private Risque risque(Projet projet, String libelle, StatutRisque statut, Utilisateur porteur) {
        Risque risque = new Risque();
        risque.setProjet(projet);
        risque.setLibelle(libelle);
        risque.setDescription("Analyse de " + libelle);
        risque.setMesureMitigation("Parade de " + libelle);
        risque.setProbabilite(4);
        risque.setImpact(4);
        risque.setNiveau(NiveauRisque.CRITIQUE);
        risque.setStatut(statut);
        risque.setPorteur(porteur);
        return risque;
    }

    private void solution(IaAnalyse analyse, Risque risque, String libelle, StatutRecommandation statut, String gain) {
        IaRecommandation solution = new IaRecommandation();
        solution.setAnalyse(analyse);
        solution.setRisque(risque);
        solution.setLibelle(libelle);
        solution.setStatut(statut);
        solution.setActionProposee(gain == null
                ? objectMapper.createObjectNode().put("type", "CREER_TACHE")
                : objectMapper.createObjectNode().put("type", "CREER_TACHE").put("gain", gain));
        iaRecommandationRepository.save(solution);
    }
}
