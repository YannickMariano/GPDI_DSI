package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mg.gov.mef.dsi.gpdi.config.TestSecurityConfig;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TraitementRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
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
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.TacheService;
import mg.gov.mef.dsi.gpdi.service.TraitementRisqueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

/**
 * Application des solutions de bout en bout : le vrai TacheService, la vraie transaction et PostgreSQL.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TraitementRisqueIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void proprietesBaseDeDonnees(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TraitementRisqueService traitementRisqueService;
    @Autowired
    private IaRecommandationRepository iaRecommandationRepository;
    @Autowired
    private IaAnalyseRepository iaAnalyseRepository;
    @Autowired
    private RisqueRepository risqueRepository;
    @Autowired
    private AffectationTacheRepository affectationTacheRepository;
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
    @Autowired
    private HabilitationRepository habilitationRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private TacheService tacheService;

    private Long idProjet;
    private Long idRisque;
    private Long idTache;
    private Long idResponsable;
    private Long idMembre;
    private Long idReaffectation;
    private Long idCreationAssignee;
    private Long idCreationHorsEquipe;

    @BeforeEach
    void preparerFixtures() {
        iaRecommandationRepository.deleteAll();
        risqueRepository.deleteAll();
        iaAnalyseRepository.deleteAll();
        affectationTacheRepository.deleteAll();
        tacheRepository.deleteAll();
        membreEquipeRepository.deleteAll();
        projetRepository.deleteAll();
        equipeRepository.deleteAll();
        utilisateurRepository.deleteAll();
        roleRepository.deleteAll();
        habilitationRepository.deleteAll();

        for (String fonction : new String[]{"Création des tâches", "Affectation des tâches",
                "Priorisation des tâches", "Changement de statut", "Enregistrement du temps passé"}) {
            Habilitation habilitation = new Habilitation();
            habilitation.setModule("Module 2");
            habilitation.setFonction(fonction);
            habilitation.setRoleNom("chef_projet");
            habilitation.setNiveau(NiveauAcces.ECRITURE);
            habilitationRepository.save(habilitation);
        }

        Role role = new Role();
        role.setNom("chef_projet");
        role.setDescription("Chef de projet");
        Role roleEnregistre = roleRepository.save(role);
        Utilisateur responsable = utilisateurRepository.save(utilisateur("93001", roleEnregistre));
        Utilisateur membre = utilisateurRepository.save(utilisateur("93002", roleEnregistre));
        idResponsable = responsable.getId();
        idMembre = membre.getId();

        Equipe equipe = new Equipe();
        equipe.setNom("Equipe Traitement");
        equipe.setChefEquipe(responsable);
        Equipe equipeEnregistree = equipeRepository.save(equipe);
        MembreEquipe membreEquipe = new MembreEquipe();
        membreEquipe.setEquipe(equipeEnregistree);
        membreEquipe.setUtilisateur(membre);
        membreEquipeRepository.save(membreEquipe);

        Projet projet = new Projet();
        projet.setTitre("Projet Traitement");
        projet.setStatut(StatutProjet.EN_COURS);
        projet.setDateDebut(LocalDate.now().minusDays(30));
        projet.setDateFin(LocalDate.now().plusDays(60));
        projet.setPriorite(Priorite.HAUTE);
        projet.setResponsable(responsable);
        projet.setEquipe(equipeEnregistree);
        Projet projetEnregistre = projetRepository.save(projet);
        idProjet = projetEnregistre.getId();

        // Le responsable n'a pas de ligne membre_equipes : le TacheService refuserait de l'assigner.
        Tache tache = new Tache();
        tache.setTitre("Migration des données");
        tache.setDescription("Description initiale");
        tache.setStatut(StatutTache.BLOQUEE);
        tache.setPriorite(Priorite.HAUTE);
        tache.setTempsEstime(40.0);
        tache.setDateFinPrevue(LocalDate.now().minusDays(3));
        tache.setProjet(projetEnregistre);
        Tache tacheEnregistree = tacheRepository.save(tache);
        idTache = tacheEnregistree.getId();
        AffectationTache affectation = new AffectationTache();
        affectation.setTache(tacheEnregistree);
        affectation.setUtilisateur(responsable);
        affectationTacheRepository.save(affectation);

        Risque risque = new Risque();
        risque.setProjet(projetEnregistre);
        risque.setLibelle("Tâches bloquées et en retard");
        risque.setProbabilite(4);
        risque.setImpact(4);
        risque.setNiveau(NiveauRisque.CRITIQUE);
        risque.setStatut(StatutRisque.OUVERT);
        Risque risqueEnregistre = risqueRepository.save(risque);
        idRisque = risqueEnregistre.getId();

        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(projetEnregistre);
        analyse.setTypeAnalyse(TypeAnalyseIa.SUGGESTION_RISQUE);
        IaAnalyse analyseEnregistree = iaAnalyseRepository.save(analyse);

        idReaffectation = solution(analyseEnregistree, risqueEnregistre, "Confier la migration au membre",
                objectMapper.createObjectNode().put("type", "REASSIGNER_TACHE").put("idTache", idTache)
                        .put("idAssigne", idMembre).put("gain", "3 jours évités"));
        idCreationAssignee = solution(analyseEnregistree, risqueEnregistre, "Planifier une revue",
                objectMapper.createObjectNode().put("type", "CREER_TACHE").put("titre", "Organiser la revue")
                        .put("description", "Revue hebdomadaire").put("tempsEstime", 2.5).put("idAssigne", idMembre));
        idCreationHorsEquipe = solution(analyseEnregistree, risqueEnregistre, "Suivre les blocages",
                objectMapper.createObjectNode().put("type", "CREER_TACHE").put("titre", "Suivre les blocages")
                        .put("idAssigne", idResponsable));

        // AccesProjetService resout l'utilisateur courant via le SecurityContext : sans cela, les appels internes
        // de TraitementRisqueService vers TacheService.creer/modifier echoueraient (aucun contexte de securite
        // n'est peuple par un appel direct au bean, contrairement a une requete HTTP reelle).
        connecterUtilisateur("93001");
    }

    @AfterEach
    void nettoyerContexteSecurite() {
        SecurityContextHolder.clearContext();
    }

    private void connecterUtilisateur(String matricule) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", matricule)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, Collections.emptyList()));
    }

    @Test
    void appliquer_executeLesSolutionsCocheesSurLeProjetEtMetLeRisqueSousControle() {
        TraitementRisqueResponse resultat = traitementRisqueService.appliquer(idProjet, idRisque,
                Arrays.asList(idCreationAssignee, idReaffectation), idResponsable);

        assertThat(resultat.getStatut()).isEqualTo("SOUS_CONTROLE");
        assertThat(resultat.getSolutions()).hasSize(2);

        List<AffectationTache> affectationsTacheInitiale = affectationTacheRepository.findByTacheId(idTache);
        assertThat(affectationsTacheInitiale).hasSize(1);
        assertThat(affectationsTacheInitiale.get(0).getUtilisateur().getId()).isEqualTo(idMembre);
        Tache tacheReaffectee = tacheRepository.findById(idTache).get();
        assertThat(tacheReaffectee.getStatut()).isEqualTo(StatutTache.BLOQUEE);
        assertThat(tacheReaffectee.getTitre()).isEqualTo("Migration des données");
        assertThat(tacheReaffectee.getTempsEstime()).isEqualTo(40.0);
        assertThat(tacheReaffectee.getDateFinPrevue()).isEqualTo(LocalDate.now().minusDays(3));

        List<Tache> taches = tacheRepository.findAll();
        assertThat(taches).hasSize(2);
        Tache creee = taches.stream().filter(t -> !t.getId().equals(idTache)).findFirst().get();
        assertThat(creee.getCode()).isEqualTo("T-" + creee.getId());
        assertThat(creee.getTitre()).isEqualTo("Organiser la revue");
        assertThat(creee.getStatut()).isEqualTo(StatutTache.A_FAIRE);
        assertThat(creee.getPriorite()).isEqualTo(Priorite.HAUTE);
        assertThat(creee.getTempsEstime()).isEqualTo(2.5);
        assertThat(creee.getDescription()).startsWith("Revue hebdomadaire").contains("« Tâches bloquées et en retard »");
        assertThat(affectationTacheRepository.findByTacheId(creee.getId())).hasSize(1);

        assertThat(risqueRepository.findById(idRisque).get().getStatut()).isEqualTo(StatutRisque.SOUS_CONTROLE);

        IaRecommandation reaffectation = iaRecommandationRepository.findById(idReaffectation).get();
        assertThat(reaffectation.getStatut()).isEqualTo(StatutRecommandation.APPLIQUEE);
        assertThat(reaffectation.getValidateur().getId()).isEqualTo(idResponsable);
        assertThat(reaffectation.getDateValidation()).isNotNull();
        assertThat(reaffectation.getResultatApplication().get("idTache").asLong()).isEqualTo(idTache);
        assertThat(reaffectation.getResultatApplication().get("typeAction").asText()).isEqualTo("REASSIGNER_TACHE");
        IaRecommandation creation = iaRecommandationRepository.findById(idCreationAssignee).get();
        assertThat(creation.getStatut()).isEqualTo(StatutRecommandation.APPLIQUEE);
        assertThat(creation.getResultatApplication().get("codeTache").asText()).isEqualTo(creee.getCode());
        IaRecommandation nonCochee = iaRecommandationRepository.findById(idCreationHorsEquipe).get();
        assertThat(nonCochee.getStatut()).isEqualTo(StatutRecommandation.REJETEE);
        assertThat(nonCochee.getMotifRejet()).contains("non retenue");
    }

    @Test
    void appliquer_creationAvecAssigneHorsEquipe_creeLaTacheSansAssignation() {
        TraitementRisqueResponse resultat = traitementRisqueService.appliquer(idProjet, idRisque,
                Arrays.asList(idCreationHorsEquipe), idResponsable);

        assertThat(resultat.getSolutions().get(0).getRemarque()).contains("sans assignation");
        Tache creee = tacheRepository.findAll().stream().filter(t -> !t.getId().equals(idTache)).findFirst().get();
        assertThat(affectationTacheRepository.findByTacheId(creee.getId())).isEmpty();
        assertThat(affectationTacheRepository.findByTacheId(idTache)).hasSize(1);
    }

    @Test
    void appliquer_uneSolutionEchoue_toutEstAnnule() {
        doCallRealMethod().doThrow(new IllegalStateException("echec simule")).when(tacheService)
                .creer(any(Long.class), any(TacheRequest.class));

        assertThatThrownBy(() -> traitementRisqueService.appliquer(idProjet, idRisque,
                Arrays.asList(idReaffectation, idCreationAssignee, idCreationHorsEquipe), idResponsable))
                .isInstanceOf(IllegalStateException.class);

        assertThat(tacheRepository.count()).isEqualTo(1);
        List<AffectationTache> affectations = affectationTacheRepository.findByTacheId(idTache);
        assertThat(affectations).hasSize(1);
        assertThat(affectations.get(0).getUtilisateur().getId()).isEqualTo(idResponsable);
        assertThat(risqueRepository.findById(idRisque).get().getStatut()).isEqualTo(StatutRisque.OUVERT);
        assertThat(iaRecommandationRepository.findAll())
                .allMatch(recommandation -> recommandation.getStatut() == StatutRecommandation.PROPOSEE);
    }

    @Test
    void rejeter_metLeRisqueAuStatutRejeteEtRejetteLesSolutionsEnAttente() {
        TraitementRisqueResponse resultat = traitementRisqueService.rejeter(idProjet, idRisque, "Hors périmètre", idResponsable);

        assertThat(resultat.getStatut()).isEqualTo("REJETE");
        assertThat(risqueRepository.findById(idRisque).get().getStatut()).isEqualTo(StatutRisque.REJETE);
        assertThat(iaRecommandationRepository.findAll()).hasSize(3).allMatch(recommandation ->
                recommandation.getStatut() == StatutRecommandation.REJETEE
                        && "Hors périmètre".equals(recommandation.getMotifRejet())
                        && recommandation.getValidateur().getId().equals(idResponsable));
        assertThat(tacheRepository.count()).isEqualTo(1);
    }

    private Long solution(IaAnalyse analyse, Risque risque, String libelle, ObjectNode action) {
        IaRecommandation solution = new IaRecommandation();
        solution.setAnalyse(analyse);
        solution.setRisque(risque);
        solution.setLibelle(libelle);
        solution.setJustification("Justification");
        solution.setActionProposee(action);
        return iaRecommandationRepository.save(solution).getId();
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
