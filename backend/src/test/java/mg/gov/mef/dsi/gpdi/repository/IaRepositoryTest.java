package mg.gov.mef.dsi.gpdi.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie contre un vrai PostgreSQL le mapping des entites du module IA : colonnes JSONB, types ENUM
 * natifs, valeurs par defaut et horodatages.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class IaRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void proprietesBaseDeDonnees(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private IaAnalyseRepository iaAnalyseRepository;
    @Autowired
    private RisqueRepository risqueRepository;
    @Autowired
    private IaRecommandationRepository iaRecommandationRepository;
    @Autowired
    private ProjetRepository projetRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private Projet projet;
    private Utilisateur porteur;

    @BeforeEach
    void nettoyerBaseEtPreparerFixtures() {
        iaRecommandationRepository.deleteAll();
        risqueRepository.deleteAll();
        iaAnalyseRepository.deleteAll();
        projetRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();

        Role role = new Role();
        role.setNom("chef_projet");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);

        Utilisateur responsable = new Utilisateur();
        responsable.setMatricule("IA001");
        responsable.setNom("Nom");
        responsable.setPrenom("Prenom");
        responsable.setEmail("ia001@test.mg");
        responsable.setMotDePasse("HASH");
        responsable.setRole(roleEnregistre);
        porteur = utilisateurRepository.save(responsable);

        Projet nouveauProjet = new Projet();
        nouveauProjet.setTitre("Projet IA");
        nouveauProjet.setDateDebut(LocalDate.now());
        nouveauProjet.setDateFin(LocalDate.now().plusMonths(3));
        nouveauProjet.setPriorite(Priorite.NORMALE);
        nouveauProjet.setResponsable(porteur);
        projet = projetRepository.save(nouveauProjet);
    }

    private IaAnalyse creerAnalyse() throws IOException {
        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(projet);
        analyse.setTypeAnalyse(TypeAnalyseIa.DETECTION_RISQUE);
        analyse.setContexte(objectMapper.readTree("{\"projet\":\"Projet IA\",\"taches\":{\"total\":40,\"enRetard\":6}}"));
        analyse.setIndiceConfiance(new BigDecimal("0.825"));
        analyse.setModele("qwen3:8b");
        return iaAnalyseRepository.save(analyse);
    }

    private Risque creerRisque(IaAnalyse analyse, String libelle) {
        Risque risque = new Risque();
        risque.setProjet(projet);
        risque.setAnalyse(analyse);
        risque.setPorteur(porteur);
        risque.setLibelle(libelle);
        risque.setDescription("Description de " + libelle);
        risque.setProbabilite(4);
        risque.setImpact(4);
        risque.setNiveau(NiveauRisque.CRITIQUE);
        return risqueRepository.save(risque);
    }

    @Test
    void enregistrerAnalyse_contexteJson_conserveLeJsonEtLesValeursParDefaut() throws IOException {
        IaAnalyse enregistree = creerAnalyse();

        IaAnalyse relue = iaAnalyseRepository.findById(enregistree.getId()).get();

        assertThat(relue.getContexte().get("projet").asText()).isEqualTo("Projet IA");
        assertThat(relue.getContexte().get("taches").get("enRetard").asInt()).isEqualTo(6);
        assertThat(relue.getIndiceConfiance()).isEqualByComparingTo("0.825");
        assertThat(relue.getTypeAnalyse()).isEqualTo(TypeAnalyseIa.DETECTION_RISQUE);
        assertThat(relue.getPortee()).isEqualTo(PorteeAnalyseIa.PROJET);
        assertThat(relue.getDemandeur()).isNull();
        assertThat(relue.getDateCreation()).isNotNull();
    }

    @Test
    void enregistrerRisque_valeursParDefaut_ouvertAvecDatesEtPorteur() throws IOException {
        Risque enregistre = creerRisque(creerAnalyse(), "Retard du referentiel");

        Risque relu = risqueRepository.findById(enregistre.getId()).get();

        assertThat(relu.getStatut()).isEqualTo(StatutRisque.OUVERT);
        assertThat(relu.getNiveau()).isEqualTo(NiveauRisque.CRITIQUE);
        assertThat(relu.getProbabilite()).isEqualTo(4);
        assertThat(relu.getPorteur().getId()).isEqualTo(porteur.getId());
        assertThat(relu.getDateCreation()).isNotNull();
        assertThat(relu.getDateMiseAJour()).isNotNull();
    }

    @Test
    void rechercherRisquesDuProjet_plusieursRisques_lesRetourneParIdCroissant() throws IOException {
        IaAnalyse analyse = creerAnalyse();
        Risque premier = creerRisque(analyse, "Premier risque");
        Risque second = creerRisque(analyse, "Second risque");

        List<Risque> risques = risqueRepository.findByProjetIdOrderByIdAsc(projet.getId());

        assertThat(risques).extracting(Risque::getId).containsExactly(premier.getId(), second.getId());
    }

    @Test
    void enregistrerRecommandation_actionEnJson_liee_auRisqueEtProposeeParDefaut() throws IOException {
        IaAnalyse analyse = creerAnalyse();
        Risque risque = creerRisque(analyse, "Risque avec solutions");
        Risque autreRisque = creerRisque(analyse, "Autre risque");

        IaRecommandation recommandation = new IaRecommandation();
        recommandation.setAnalyse(analyse);
        recommandation.setRisque(risque);
        recommandation.setLibelle("Reaffecter un developpeur");
        recommandation.setJustification("Charge de l'equipe trop elevee");
        JsonNode action = objectMapper.readTree(
                "{\"type\":\"REASSIGNER_TACHE\",\"gain\":\"-3 j de retard\",\"parametres\":{\"idTache\":149}}");
        recommandation.setActionProposee(action);
        IaRecommandation enregistree = iaRecommandationRepository.save(recommandation);

        List<IaRecommandation> duRisque = iaRecommandationRepository.findByRisqueIdOrderByIdAsc(risque.getId());
        List<IaRecommandation> duAutreRisque = iaRecommandationRepository.findByRisqueIdOrderByIdAsc(autreRisque.getId());

        assertThat(duRisque).hasSize(1);
        assertThat(duAutreRisque).isEmpty();
        IaRecommandation relue = iaRecommandationRepository.findById(enregistree.getId()).get();
        assertThat(relue.getStatut()).isEqualTo(StatutRecommandation.PROPOSEE);
        assertThat(relue.getActionProposee().get("type").asText()).isEqualTo("REASSIGNER_TACHE");
        assertThat(relue.getActionProposee().get("parametres").get("idTache").asLong()).isEqualTo(149L);
        assertThat(relue.getResultatApplication()).isNull();
        assertThat(relue.getValidateur()).isNull();
        assertThat(relue.getDateCreation()).isNotNull();
    }
}
