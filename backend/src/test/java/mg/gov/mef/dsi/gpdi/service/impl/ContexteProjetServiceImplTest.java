package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.StatutSprint;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.BurndownSnapshotRepository;
import mg.gov.mef.dsi.gpdi.repository.JalonRepository;
import mg.gov.mef.dsi.gpdi.repository.LivrableRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.SprintRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContexteProjetServiceImplTest {

    private static final Long ID_PROJET = 1L;

    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private AffectationTacheRepository affectationTacheRepository;
    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private BurndownSnapshotRepository burndownSnapshotRepository;
    @Mock
    private JalonRepository jalonRepository;
    @Mock
    private LivrableRepository livrableRepository;
    @Mock
    private MembreEquipeRepository membreEquipeRepository;
    @Mock
    private RisqueRepository risqueRepository;

    private ContexteProjetServiceImpl service;
    private Projet projet;
    private Utilisateur responsable;
    private Utilisateur chef;
    private Utilisateur membre;

    @BeforeEach
    void setUp() {
        service = new ContexteProjetServiceImpl(projetRepository, tacheRepository, affectationTacheRepository,
                sprintRepository, burndownSnapshotRepository, jalonRepository, livrableRepository,
                membreEquipeRepository, risqueRepository, new ObjectMapper());

        responsable = utilisateur(1L, "Rakoto", "Jean");
        chef = utilisateur(2L, "Rabe", "Marie");
        membre = utilisateur(3L, "Randria", "Paul");

        Equipe equipe = new Equipe();
        equipe.setId(7L);
        equipe.setChefEquipe(chef);

        projet = new Projet();
        projet.setId(ID_PROJET);
        projet.setTitre("Refonte du SI budgétaire");
        projet.setStatut(StatutProjet.EN_COURS);
        projet.setPriorite(Priorite.HAUTE);
        projet.setDateDebut(LocalDate.now().minusDays(60));
        projet.setDateFin(LocalDate.now().plusDays(30));
        projet.setProgression(40);
        projet.setResponsable(responsable);
        projet.setEquipe(equipe);

        MembreEquipe membreActif = membreEquipe(equipe, membre, true);
        MembreEquipe membreInactif = membreEquipe(equipe, utilisateur(4L, "Inactif", "Ancien"), false);

        lenient().when(projetRepository.findById(ID_PROJET)).thenReturn(java.util.Optional.of(projet));
        lenient().when(membreEquipeRepository.findByEquipeId(7L)).thenReturn(Arrays.asList(membreActif, membreInactif));
        lenient().when(tacheRepository.findByProjetId(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<Tache>(Collections.<Tache>emptyList()));
        lenient().when(jalonRepository.findByProjetId(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<mg.gov.mef.dsi.gpdi.entity.Jalon>(Collections.<mg.gov.mef.dsi.gpdi.entity.Jalon>emptyList()));
        lenient().when(risqueRepository.findByProjetIdOrderByIdAsc(ID_PROJET)).thenReturn(Collections.<Risque>emptyList());
    }

    @Test
    void construire_projetInconnu_leveProjetIntrouvableException() {
        when(projetRepository.findById(99L)).thenReturn(java.util.Optional.<Projet>empty());

        assertThatThrownBy(() -> service.construire(99L)).isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void construire_decritLeProjetEtLEquipeSansDonneesPersonnelles() {
        ContexteProjetIa contexte = service.construire(ID_PROJET);

        JsonNode racine = contexte.getContenu();
        assertThat(racine.get("dateDuJour").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(racine.get("projet").get("titre").asText()).isEqualTo("Refonte du SI budgétaire");
        assertThat(racine.get("projet").get("joursAvantEcheance").asLong()).isEqualTo(30L);
        // Projet sans tache : progression calculee a 0, la colonne stockee n'est pas lue.
        assertThat(racine.get("projet").get("progressionPourcent").asInt()).isZero();

        JsonNode equipe = racine.get("equipe");
        assertThat(equipe).hasSize(3);
        assertThat(equipe.get(0).get("fonction").asText()).isEqualTo("RESPONSABLE_PROJET");
        assertThat(equipe.get(1).get("fonction").asText()).isEqualTo("CHEF_EQUIPE");
        assertThat(equipe.get(2).get("fonction").asText()).isEqualTo("MEMBRE");
        assertThat(equipe.get(0).get("nom").asText()).isEqualTo("Jean Rakoto");
        assertThat(racine.toString()).doesNotContain("email").doesNotContain("matricule").doesNotContain("telephone");
        assertThat(contexte.getIdsPorteursPossibles()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void construire_utilisateurCumulantResponsableEtChef_apparaitUneSeuleFois() {
        projet.getEquipe().setChefEquipe(responsable);

        ContexteProjetIa contexte = service.construire(ID_PROJET);

        assertThat(contexte.getContenu().get("equipe")).hasSize(2);
        assertThat(contexte.getContenu().get("equipe").get(0).get("fonction").asText()).isEqualTo("RESPONSABLE_PROJET");
    }

    @Test
    void construire_tachesEnRetardBloqueesOuSansAssigne_sontComptabiliseesEtSignalees() {
        Tache enRetard = tache(10L, "T-10", StatutTache.EN_COURS, LocalDate.now().minusDays(3), 10.0, 4.0);
        Tache bloquee = tache(11L, "T-11", StatutTache.BLOQUEE, LocalDate.now().plusDays(20), 5.0, 0.0);
        Tache terminee = tache(12L, "T-12", StatutTache.TERMINEE, LocalDate.now().minusDays(10), 8.0, 8.0);
        Tache lointaine = tache(13L, "T-13", StatutTache.A_FAIRE, LocalDate.now().plusDays(40), null, null);
        when(tacheRepository.findByProjetId(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<Tache>(Arrays.asList(lointaine, terminee, enRetard, bloquee)));
        when(affectationTacheRepository.findByTacheIdIn(anyList()))
                .thenReturn(Arrays.asList(affectation(enRetard, membre), affectation(terminee, membre)));

        JsonNode taches = service.construire(ID_PROJET).getContenu().get("taches");

        assertThat(taches.get("total").asInt()).isEqualTo(4);
        assertThat(taches.get("enRetard").asInt()).isEqualTo(1);
        assertThat(taches.get("sansAssigne").asInt()).isEqualTo(2);
        assertThat(taches.get("sansEstimation").asInt()).isEqualTo(1);
        assertThat(taches.get("dansLeBacklog").asInt()).isEqualTo(3);
        assertThat(taches.get("parStatut").get("TERMINEE").asInt()).isEqualTo(1);

        JsonNode aSurveiller = taches.get("aSurveiller");
        assertThat(aSurveiller).hasSize(2);
        assertThat(aSurveiller.get(0).get("code").asText()).isEqualTo("T-11");
        assertThat(aSurveiller.get(1).get("code").asText()).isEqualTo("T-10");
        assertThat(aSurveiller.get(1).get("joursDeRetard").asLong()).isEqualTo(3L);
        assertThat(aSurveiller.get(1).get("assignes").get(0).asLong()).isEqualTo(3L);
    }

    @Test
    void construire_chargeDeLEquipe_neCompteQueLesTachesOuvertes() {
        Tache ouverte = tache(10L, "T-10", StatutTache.EN_COURS, LocalDate.now().plusDays(2), 10.0, 4.0);
        Tache terminee = tache(12L, "T-12", StatutTache.TERMINEE, LocalDate.now().minusDays(10), 8.0, 8.0);
        when(tacheRepository.findByProjetId(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<Tache>(Arrays.asList(ouverte, terminee)));
        when(affectationTacheRepository.findByTacheIdIn(anyList()))
                .thenReturn(Arrays.asList(affectation(ouverte, membre), affectation(terminee, membre)));

        JsonNode equipe = service.construire(ID_PROJET).getContenu().get("equipe");

        JsonNode noeudMembre = equipe.get(2);
        assertThat(noeudMembre.get("id").asLong()).isEqualTo(3L);
        assertThat(noeudMembre.get("tachesOuvertes").asInt()).isEqualTo(1);
        assertThat(noeudMembre.get("heuresRestantes").asDouble()).isEqualTo(6.0);
    }

    @Test
    void construire_listeDesTachesASurveiller_estPlafonneeA15() {
        List<Tache> taches = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            taches.add(tache(i, "T-" + i, StatutTache.EN_COURS, LocalDate.now().minusDays(i), 2.0, 0.0));
        }
        when(tacheRepository.findByProjetId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<Tache>(taches));

        JsonNode taches15 = service.construire(ID_PROJET).getContenu().get("taches");

        assertThat(taches15.get("enRetard").asInt()).isEqualTo(20);
        assertThat(taches15.get("aSurveiller")).hasSize(15);
    }

    @Test
    void construire_risques_reevaluablesSontLesSeulsSousControle() {
        Risque ouvert = risque(1L, "Risque ouvert", StatutRisque.OUVERT);
        Risque sousControle = risque(2L, "Risque sous contrôle", StatutRisque.SOUS_CONTROLE);
        Risque rejete = risque(3L, "Risque rejeté", StatutRisque.REJETE);
        when(risqueRepository.findByProjetIdOrderByIdAsc(ID_PROJET)).thenReturn(Arrays.asList(ouvert, sousControle, rejete));

        ContexteProjetIa contexte = service.construire(ID_PROJET);

        assertThat(contexte.getIdsRisquesReevaluables()).containsExactly(2L);
        assertThat(contexte.getLibellesRisquesConnus())
                .containsExactly("Risque ouvert", "Risque sous contrôle", "Risque rejeté");
        assertThat(contexte.getContenu().get("risquesConnus")).hasSize(3);
    }

    @Test
    void construire_risquesTropNombreux_gardeTousLesSousControleEtLesPlusRecentsDesAutres() {
        List<Risque> risques = new ArrayList<>();
        for (long i = 1; i <= 40; i++) {
            risques.add(risque(i, "Risque numero " + i, i == 1 ? StatutRisque.SOUS_CONTROLE : StatutRisque.OUVERT));
        }
        when(risqueRepository.findByProjetIdOrderByIdAsc(ID_PROJET)).thenReturn(risques);

        ContexteProjetIa contexte = service.construire(ID_PROJET);

        JsonNode liste = contexte.getContenu().get("risquesConnus");
        assertThat(liste).hasSize(30);
        assertThat(liste.get(0).get("id").asLong()).isEqualTo(1L);
        assertThat(liste.get(29).get("id").asLong()).isEqualTo(40L);
        assertThat(contexte.getLibellesRisquesConnus()).hasSize(40);
    }

    @Test
    void construire_sprintEnCours_exposeLaCapaciteEtLesHeuresRestantes() {
        Sprint sprint = new Sprint();
        sprint.setId(20L);
        sprint.setNom("Sprint 1");
        sprint.setStatut(StatutSprint.EN_COURS);
        sprint.setDateDebut(LocalDate.now().minusDays(5));
        sprint.setDateFin(LocalDate.now().plusDays(5));
        sprint.setCapaciteHeures(80);
        Tache tache = tache(10L, "T-10", StatutTache.EN_COURS, LocalDate.now().plusDays(2), 10.0, 4.0);
        tache.setSprint(sprint);
        when(tacheRepository.findByProjetId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<Tache>(Collections.singletonList(tache)));
        when(sprintRepository.findByProjetId(ID_PROJET)).thenReturn(Collections.singletonList(sprint));
        when(burndownSnapshotRepository.findBySprintIdOrderByDateAsc(20L)).thenReturn(Collections.<mg.gov.mef.dsi.gpdi.entity.BurndownSnapshot>emptyList());

        JsonNode sprints = service.construire(ID_PROJET).getContenu().get("sprints");

        assertThat(sprints).hasSize(1);
        assertThat(sprints.get(0).get("capaciteHeures").asInt()).isEqualTo(80);
        assertThat(sprints.get(0).get("heuresRestantes").asDouble()).isEqualTo(6.0);
        assertThat(sprints.get(0).get("burndown")).isEmpty();
    }

    @Test
    void construire_progressionCalculeeAPartirDesTachesTerminees() {
        when(tacheRepository.findByProjetId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<Tache>(Arrays.asList(
                tache(1L, "T-1", StatutTache.TERMINEE, null, 1.0, 1.0),
                tache(2L, "T-2", StatutTache.TERMINEE, null, 1.0, 1.0),
                tache(3L, "T-3", StatutTache.A_FAIRE, null, 1.0, 0.0),
                tache(4L, "T-4", StatutTache.EN_COURS, null, 1.0, 0.0),
                tache(5L, "T-5", StatutTache.EN_TEST, null, 1.0, 0.0))));

        JsonNode projetIa = service.construire(ID_PROJET).getContenu().get("projet");

        assertThat(projetIa.get("progressionPourcent").asInt()).isEqualTo(40);
    }

    private Utilisateur utilisateur(Long id, String nom, String prenom) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(nom.toLowerCase() + "@mef.gov.mg");
        utilisateur.setMatricule("MAT-" + id);
        utilisateur.setTelephone("0340000000");
        Role role = new Role();
        role.setNom("membre_equipe");
        utilisateur.setRole(role);
        return utilisateur;
    }

    private MembreEquipe membreEquipe(Equipe equipe, Utilisateur utilisateur, boolean actif) {
        MembreEquipe membreEquipe = new MembreEquipe();
        membreEquipe.setEquipe(equipe);
        membreEquipe.setUtilisateur(utilisateur);
        membreEquipe.setActif(actif);
        return membreEquipe;
    }

    private Tache tache(Long id, String code, StatutTache statut, LocalDate echeance, Double estime, Double passe) {
        Tache tache = new Tache();
        tache.setId(id);
        tache.setCode(code);
        tache.setTitre("Tache " + code);
        tache.setStatut(statut);
        tache.setDateFinPrevue(echeance);
        tache.setTempsEstime(estime);
        tache.setTempsPasse(passe);
        tache.setProjet(projet);
        return tache;
    }

    private AffectationTache affectation(Tache tache, Utilisateur utilisateur) {
        AffectationTache affectation = new AffectationTache();
        affectation.setTache(tache);
        affectation.setUtilisateur(utilisateur);
        return affectation;
    }

    private Risque risque(Long id, String libelle, StatutRisque statut) {
        Risque risque = new Risque();
        risque.setId(id);
        risque.setLibelle(libelle);
        risque.setStatut(statut);
        risque.setNiveau(NiveauRisque.MOYEN);
        risque.setProbabilite(3);
        risque.setImpact(3);
        return risque;
    }
}
