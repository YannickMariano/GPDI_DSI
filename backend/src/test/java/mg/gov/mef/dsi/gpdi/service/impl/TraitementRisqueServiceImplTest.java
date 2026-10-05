package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TraitementRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.Sprint;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.RecommandationsConflictuellesException;
import mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.SolutionRisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRecommandationInvalideException;
import mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.TacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraitementRisqueServiceImplTest {

    private static final Long ID_PROJET = 1L;
    private static final Long ID_RISQUE = 5L;
    private static final Long ID_EQUIPE = 3L;
    private static final Long ID_VALIDATEUR = 50L;
    private static final Long ID_MEMBRE = 11L;
    private static final Long ID_TACHE = 100L;
    private static final Long ID_AUTRE_TACHE = 101L;

    @Mock
    private RisqueRepository risqueRepository;
    @Mock
    private IaRecommandationRepository iaRecommandationRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private MembreEquipeRepository membreEquipeRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private TacheService tacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TraitementRisqueServiceImpl service;
    private Projet projet;
    private Risque risque;
    private Utilisateur validateur;

    @BeforeEach
    void setUp() {
        service = new TraitementRisqueServiceImpl(risqueRepository, iaRecommandationRepository, tacheRepository,
                membreEquipeRepository, utilisateurRepository, tacheService, objectMapper);

        Equipe equipe = new Equipe();
        equipe.setId(ID_EQUIPE);
        projet = new Projet();
        projet.setId(ID_PROJET);
        projet.setEquipe(equipe);
        risque = new Risque();
        risque.setId(ID_RISQUE);
        risque.setProjet(projet);
        risque.setLibelle("Tâches bloquées");
        risque.setNiveau(NiveauRisque.CRITIQUE);
        risque.setStatut(StatutRisque.OUVERT);
        validateur = new Utilisateur();
        validateur.setId(ID_VALIDATEUR);

        lenient().when(risqueRepository.findByIdForUpdate(ID_RISQUE)).thenReturn(Optional.of(risque));
        lenient().when(utilisateurRepository.findById(ID_VALIDATEUR)).thenReturn(Optional.of(validateur));
        lenient().when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(ID_EQUIPE, ID_MEMBRE)).thenReturn(true);
        lenient().when(tacheService.creer(eq(ID_PROJET), any(TacheRequest.class))).thenReturn(reponseTache(200L, "T-200"));
    }

    @Test
    void appliquer_reaffectation_conserveLesChampsDeLaTacheEtChangeLAssigne() {
        Sprint sprint = new Sprint();
        sprint.setId(7L);
        Tache tache = tache(StatutTache.EN_COURS);
        tache.setSprint(sprint);
        when(tacheRepository.findById(ID_TACHE)).thenReturn(Optional.of(tache));
        when(tacheService.modifier(eq(ID_PROJET), eq(ID_TACHE), any(TacheRequest.class))).thenReturn(reponseTache(ID_TACHE, "T-100"));
        IaRecommandation solution = solution(31L, reaffectation(ID_TACHE, ID_MEMBRE));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Collections.singletonList(solution));

        TraitementRisqueResponse resultat = service.appliquer(ID_PROJET, ID_RISQUE, Collections.singletonList(31L), ID_VALIDATEUR);

        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).modifier(eq(ID_PROJET), eq(ID_TACHE), requete.capture());
        assertThat(requete.getValue().getTitre()).isEqualTo("Migration");
        assertThat(requete.getValue().getDescription()).isEqualTo("Description");
        assertThat(requete.getValue().getPriorite()).isEqualTo(Priorite.HAUTE);
        assertThat(requete.getValue().getTempsEstime()).isEqualTo(40.0);
        assertThat(requete.getValue().getIdSprint()).isEqualTo(7L);
        assertThat(requete.getValue().getDateEcheance()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(requete.getValue().getIdsUtilisateursAssignes()).containsExactly(ID_MEMBRE);
        assertThat(requete.getValue().getStatut()).isNull();
        assertThat(requete.getValue().getTempsPasse()).isNull();

        assertThat(resultat.getStatut()).isEqualTo("SOUS_CONTROLE");
        assertThat(resultat.getSolutions()).hasSize(1);
        assertThat(resultat.getSolutions().get(0).getTypeAction()).isEqualTo("REASSIGNER_TACHE");
        assertThat(resultat.getSolutions().get(0).getCodeTache()).isEqualTo("T-100");
        assertThat(resultat.getSolutions().get(0).isSuivi()).isFalse();
        assertThat(risque.getStatut()).isEqualTo(StatutRisque.SOUS_CONTROLE);
        verify(risqueRepository).save(risque);
        assertThat(solution.getStatut()).isEqualTo(StatutRecommandation.APPLIQUEE);
        assertThat(solution.getValidateur()).isSameAs(validateur);
        assertThat(solution.getDateValidation()).isNotNull();
        assertThat(solution.getResultatApplication().get("idTache").asLong()).isEqualTo(ID_TACHE);
        assertThat(solution.getResultatApplication().get("codeTache").asText()).isEqualTo("T-100");
        assertThat(solution.getResultatApplication().get("suivi").asBoolean()).isFalse();
    }

    @Test
    void appliquer_creationDeTache_utiliseLesDonneesDeLActionEtLaPrioriteDuRisque() {
        IaRecommandation solution = solution(31L, creation("Organiser la revue", "Revue hebdo", 3.5, ID_MEMBRE));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Collections.singletonList(solution));

        TraitementRisqueResponse resultat = service.appliquer(ID_PROJET, ID_RISQUE, Collections.singletonList(31L), ID_VALIDATEUR);

        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).creer(eq(ID_PROJET), requete.capture());
        assertThat(requete.getValue().getTitre()).isEqualTo("Organiser la revue");
        assertThat(requete.getValue().getDescription()).startsWith("Revue hebdo").contains("« Tâches bloquées »");
        assertThat(requete.getValue().getPriorite()).isEqualTo(Priorite.HAUTE);
        assertThat(requete.getValue().getTempsEstime()).isEqualTo(3.5);
        assertThat(requete.getValue().getIdsUtilisateursAssignes()).containsExactly(ID_MEMBRE);
        assertThat(requete.getValue().getIdSprint()).isNull();
        assertThat(resultat.getSolutions().get(0).getTypeAction()).isEqualTo("CREER_TACHE");
        assertThat(resultat.getSolutions().get(0).getIdTache()).isEqualTo(200L);
        assertThat(resultat.getSolutions().get(0).isSuivi()).isFalse();
        assertThat(resultat.getSolutions().get(0).getRemarque()).isNull();
    }

    @Test
    void appliquer_risqueDeNiveauFaible_creeUneTacheDePrioriteNormale() {
        risque.setNiveau(NiveauRisque.MOYEN);
        IaRecommandation solution = solution(31L, creation("Suivi", null, null, null));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Collections.singletonList(solution));

        service.appliquer(ID_PROJET, ID_RISQUE, Collections.singletonList(31L), ID_VALIDATEUR);

        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).creer(eq(ID_PROJET), requete.capture());
        assertThat(requete.getValue().getPriorite()).isEqualTo(Priorite.NORMALE);
        assertThat(requete.getValue().getTempsEstime()).isNull();
        assertThat(requete.getValue().getIdsUtilisateursAssignes()).isEmpty();
        assertThat(requete.getValue().getDescription()).contains("« Tâches bloquées »");
    }

    @Test
    void appliquer_creationAvecMembreHorsEquipe_creeLaTacheSansAssignation() {
        IaRecommandation solution = solution(31L, creation("Organiser la revue", "Revue", 2.0, 999L));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Collections.singletonList(solution));

        TraitementRisqueResponse resultat = service.appliquer(ID_PROJET, ID_RISQUE, Collections.singletonList(31L), ID_VALIDATEUR);

        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).creer(eq(ID_PROJET), requete.capture());
        assertThat(requete.getValue().getIdsUtilisateursAssignes()).isEmpty();
        assertThat(resultat.getSolutions().get(0).getRemarque()).contains("sans assignation");
        assertThat(resultat.getSolutions().get(0).isSuivi()).isFalse();
    }

    @Test
    void appliquer_reaffectationDeTacheCloturee_devientUneTacheDeSuivi() {
        when(tacheRepository.findById(ID_TACHE)).thenReturn(Optional.of(tache(StatutTache.TERMINEE)));
        IaRecommandation solution = solution(31L, reaffectation(ID_TACHE, ID_MEMBRE));
        solution.setLibelle("Confier T-100 à Lala");
        solution.setJustification("Elle est disponible");
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Collections.singletonList(solution));

        TraitementRisqueResponse resultat = service.appliquer(ID_PROJET, ID_RISQUE, Collections.singletonList(31L), ID_VALIDATEUR);

        verify(tacheService, never()).modifier(any(Long.class), any(Long.class), any(TacheRequest.class));
        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).creer(eq(ID_PROJET), requete.capture());
        assertThat(requete.getValue().getTitre()).isEqualTo("Confier T-100 à Lala");
        assertThat(requete.getValue().getDescription()).startsWith("Elle est disponible");
        assertThat(requete.getValue().getIdsUtilisateursAssignes()).containsExactly(ID_MEMBRE);
        assertThat(resultat.getSolutions().get(0).getTypeAction()).isEqualTo("CREER_TACHE");
        assertThat(resultat.getSolutions().get(0).isSuivi()).isTrue();
        assertThat(resultat.getSolutions().get(0).getRemarque()).contains("Réaffectation impossible");
        assertThat(solution.getResultatApplication().get("suivi").asBoolean()).isTrue();
    }

    @Test
    void appliquer_reaffectationVersMembreHorsEquipeOuTacheDUnAutreProjet_devientUneTacheDeSuivi() {
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        Tache tacheAutreProjet = tache(StatutTache.EN_COURS);
        tacheAutreProjet.setId(ID_AUTRE_TACHE);
        tacheAutreProjet.setProjet(autreProjet);
        when(tacheRepository.findById(ID_AUTRE_TACHE)).thenReturn(Optional.of(tacheAutreProjet));
        // Deux taches differentes : ce test verifie deux causes de repli distinctes, pas un conflit entre solutions.
        IaRecommandation membreInconnu = solution(31L, reaffectation(ID_TACHE, 999L));
        IaRecommandation autreProjetSolution = solution(32L, reaffectation(ID_AUTRE_TACHE, ID_MEMBRE));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Arrays.asList(membreInconnu, autreProjetSolution));

        TraitementRisqueResponse resultat = service.appliquer(ID_PROJET, ID_RISQUE, Arrays.asList(31L, 32L), ID_VALIDATEUR);

        verify(tacheService, never()).modifier(any(Long.class), any(Long.class), any(TacheRequest.class));
        assertThat(resultat.getSolutions()).extracting("suivi").containsExactly(true, true);
    }

    @Test
    void appliquer_deuxSolutionsCocheesReaffectentLaMemeTache_leveRecommandationsConflictuellesException() {
        Tache tache = tache(StatutTache.EN_COURS);
        tache.setCode("T-100");
        when(tacheRepository.findById(ID_TACHE)).thenReturn(Optional.of(tache));
        // Deux solutions distinctes proposent de reaffecter la meme tache a deux agents differents : cocher les deux
        // les ferait s'ecraser silencieusement l'une l'autre, ce qui doit etre refuse avant toute execution.
        IaRecommandation versMembre = solution(31L, reaffectation(ID_TACHE, ID_MEMBRE));
        IaRecommandation versAutreMembre = solution(32L, reaffectation(ID_TACHE, 12L));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Arrays.asList(versMembre, versAutreMembre));

        assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RISQUE, Arrays.asList(31L, 32L), ID_VALIDATEUR))
                .isInstanceOf(RecommandationsConflictuellesException.class)
                .hasMessageContaining("T-100");

        verifyNoInteractions(tacheService);
        assertThat(versMembre.getStatut()).isEqualTo(StatutRecommandation.PROPOSEE);
        assertThat(risque.getStatut()).isEqualTo(StatutRisque.OUVERT);
    }

    @Test
    void appliquer_solutionsNonCochees_sontEcartees() {
        IaRecommandation cochee = solution(31L, creation("A", null, null, null));
        IaRecommandation nonCochee = solution(32L, creation("B", null, null, null));
        IaRecommandation dejaRejetee = solution(33L, creation("C", null, null, null));
        dejaRejetee.setStatut(StatutRecommandation.REJETEE);
        dejaRejetee.setMotifRejet("ancien motif");
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Arrays.asList(cochee, nonCochee, dejaRejetee));

        service.appliquer(ID_PROJET, ID_RISQUE, Collections.singletonList(31L), ID_VALIDATEUR);

        assertThat(cochee.getStatut()).isEqualTo(StatutRecommandation.APPLIQUEE);
        assertThat(nonCochee.getStatut()).isEqualTo(StatutRecommandation.REJETEE);
        assertThat(nonCochee.getMotifRejet()).contains("non retenue");
        assertThat(nonCochee.getValidateur()).isSameAs(validateur);
        assertThat(dejaRejetee.getMotifRejet()).isEqualTo("ancien motif");
    }

    @Test
    void appliquer_plusieursSolutions_sontExecuteesParIdentifiantCroissantSansDoublon() {
        IaRecommandation premiere = solution(31L, creation("A", null, null, null));
        IaRecommandation seconde = solution(32L, creation("B", null, null, null));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Arrays.asList(premiere, seconde));

        TraitementRisqueResponse resultat = service.appliquer(ID_PROJET, ID_RISQUE, Arrays.asList(32L, 31L, 32L), ID_VALIDATEUR);

        assertThat(resultat.getSolutions()).extracting("id").containsExactly(31L, 32L);
        verify(tacheService, org.mockito.Mockito.times(2)).creer(eq(ID_PROJET), any(TacheRequest.class));
    }

    @Test
    void appliquer_solutionInconnueOuAutreStatut_neRienExecuter() {
        IaRecommandation dejaAppliquee = solution(33L, creation("C", null, null, null));
        dejaAppliquee.setStatut(StatutRecommandation.APPLIQUEE);
        IaRecommandation valide = solution(31L, creation("A", null, null, null));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Arrays.asList(valide, dejaAppliquee));

        assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RISQUE, Arrays.asList(31L, 99L), ID_VALIDATEUR))
                .isInstanceOf(SolutionRisqueIntrouvableException.class);
        assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RISQUE, Arrays.asList(31L, 33L), ID_VALIDATEUR))
                .isInstanceOf(StatutRecommandationInvalideException.class);

        verifyNoInteractions(tacheService);
        assertThat(risque.getStatut()).isEqualTo(StatutRisque.OUVERT);
        assertThat(valide.getStatut()).isEqualTo(StatutRecommandation.PROPOSEE);
    }

    @Test
    void appliquer_risqueInconnuOuNonOuvert_leveLExceptionAttendue() {
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        when(risqueRepository.findByIdForUpdate(99L)).thenReturn(Optional.<Risque>empty());

        assertThatThrownBy(() -> service.appliquer(ID_PROJET, 99L, Collections.singletonList(1L), ID_VALIDATEUR))
                .isInstanceOf(RisqueIntrouvableException.class);
        assertThatThrownBy(() -> service.appliquer(2L, ID_RISQUE, Collections.singletonList(1L), ID_VALIDATEUR))
                .isInstanceOf(RisqueIntrouvableException.class);
        for (StatutRisque statut : new StatutRisque[]{StatutRisque.SOUS_CONTROLE, StatutRisque.REJETE, StatutRisque.CLOS}) {
            risque.setStatut(statut);
            assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RISQUE, Collections.singletonList(1L), ID_VALIDATEUR))
                    .isInstanceOf(StatutRisqueInvalideException.class);
        }

        verifyNoInteractions(tacheService);
    }

    @Test
    void rejeter_risqueOuvert_leRejetteAinsiQueSesSolutionsEnAttente() {
        IaRecommandation enAttente = solution(31L, creation("A", null, null, null));
        IaRecommandation dejaAppliquee = solution(32L, creation("B", null, null, null));
        dejaAppliquee.setStatut(StatutRecommandation.APPLIQUEE);
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Arrays.asList(enAttente, dejaAppliquee));

        TraitementRisqueResponse resultat = service.rejeter(ID_PROJET, ID_RISQUE, "  Hors périmètre  ", ID_VALIDATEUR);

        assertThat(resultat.getStatut()).isEqualTo("REJETE");
        assertThat(resultat.getSolutions()).isEmpty();
        assertThat(risque.getStatut()).isEqualTo(StatutRisque.REJETE);
        verify(risqueRepository).save(risque);
        assertThat(enAttente.getStatut()).isEqualTo(StatutRecommandation.REJETEE);
        assertThat(enAttente.getMotifRejet()).isEqualTo("Hors périmètre");
        assertThat(enAttente.getValidateur()).isSameAs(validateur);
        assertThat(enAttente.getDateValidation()).isNotNull();
        assertThat(dejaAppliquee.getStatut()).isEqualTo(StatutRecommandation.APPLIQUEE);
        verifyNoInteractions(tacheService);
    }

    @Test
    void rejeter_sansMotifOuMotifVide_neConserveAucunMotif() {
        IaRecommandation solution = solution(31L, creation("A", null, null, null));
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Collections.singletonList(solution));

        service.rejeter(ID_PROJET, ID_RISQUE, "   ", ID_VALIDATEUR);

        assertThat(solution.getMotifRejet()).isNull();
        assertThat(risque.getStatut()).isEqualTo(StatutRisque.REJETE);
    }

    @Test
    void rejeter_risqueNonOuvertOuInconnu_leveLExceptionAttendue() {
        risque.setStatut(StatutRisque.SOUS_CONTROLE);
        when(risqueRepository.findByIdForUpdate(99L)).thenReturn(Optional.<Risque>empty());

        assertThatThrownBy(() -> service.rejeter(ID_PROJET, ID_RISQUE, null, ID_VALIDATEUR))
                .isInstanceOf(StatutRisqueInvalideException.class);
        assertThatThrownBy(() -> service.rejeter(ID_PROJET, 99L, null, ID_VALIDATEUR))
                .isInstanceOf(RisqueIntrouvableException.class);
        verify(risqueRepository, never()).save(any(Risque.class));
    }

    private IaRecommandation solution(Long id, ObjectNode action) {
        IaRecommandation solution = new IaRecommandation();
        solution.setId(id);
        solution.setRisque(risque);
        solution.setLibelle("Solution " + id);
        solution.setStatut(StatutRecommandation.PROPOSEE);
        solution.setActionProposee(action);
        return solution;
    }

    private ObjectNode reaffectation(Long idTache, Long idAssigne) {
        return objectMapper.createObjectNode().put("type", "REASSIGNER_TACHE").put("idTache", idTache)
                .put("idAssigne", idAssigne).put("gain", "1 jour");
    }

    private ObjectNode creation(String titre, String description, Double tempsEstime, Long idAssigne) {
        ObjectNode action = objectMapper.createObjectNode().put("type", "CREER_TACHE").put("titre", titre);
        if (description != null) {
            action.put("description", description);
        }
        if (tempsEstime != null) {
            action.put("tempsEstime", tempsEstime);
        }
        if (idAssigne != null) {
            action.put("idAssigne", idAssigne);
        }
        return action;
    }

    private Tache tache(StatutTache statut) {
        Tache tache = new Tache();
        tache.setId(ID_TACHE);
        tache.setProjet(projet);
        tache.setTitre("Migration");
        tache.setDescription("Description");
        tache.setPriorite(Priorite.HAUTE);
        tache.setTempsEstime(40.0);
        tache.setDateFinPrevue(LocalDate.of(2026, 10, 1));
        tache.setStatut(statut);
        return tache;
    }

    private TacheResponse reponseTache(Long id, String code) {
        TacheResponse reponse = new TacheResponse();
        reponse.setId(id);
        reponse.setCode(code);
        return reponse;
    }
}
