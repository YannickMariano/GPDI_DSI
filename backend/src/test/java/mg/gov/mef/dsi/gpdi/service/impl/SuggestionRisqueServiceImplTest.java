package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseSuggestionRisque.SolutionProposee;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandation;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException;
import mg.gov.mef.dsi.gpdi.exception.RisqueIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRisqueInvalideException;
import mg.gov.mef.dsi.gpdi.exception.SuggestionDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.mapper.IaRecommandationMapper;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.service.ReponseSuggestionRisqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestionRisqueServiceImplTest {

    private static final Long ID_PROJET = 1L;
    private static final Long ID_RISQUE = 5L;
    private static final Long ID_DEMANDEUR = 50L;
    private static final String REPONSE_BRUTE = "{\"brut\":true}";

    @Mock
    private ContexteProjetService contexteProjetService;
    @Mock
    private ReponseSuggestionRisqueService reponseSuggestionRisqueService;
    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private RisqueRepository risqueRepository;
    @Mock
    private IaAnalyseRepository iaAnalyseRepository;
    @Mock
    private IaRecommandationRepository iaRecommandationRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SuggestionRisqueServiceImpl service;
    private Projet projet;
    private Risque risque;
    private ContexteProjetIa contexte;

    @BeforeEach
    void setUp() throws IOException {
        service = new SuggestionRisqueServiceImpl(contexteProjetService, reponseSuggestionRisqueService, ollamaClient,
                risqueRepository, iaAnalyseRepository, iaRecommandationRepository, utilisateurRepository,
                Mappers.getMapper(IaRecommandationMapper.class), objectMapper, new TransactionTemplate(transactionManager));

        projet = new Projet();
        projet.setId(ID_PROJET);
        risque = new Risque();
        risque.setId(ID_RISQUE);
        risque.setProjet(projet);
        risque.setLibelle("Tâches bloquées");
        risque.setProbabilite(4);
        risque.setImpact(4);
        risque.setNiveau(NiveauRisque.CRITIQUE);
        risque.setStatut(StatutRisque.OUVERT);

        JsonNode contenu = objectMapper.readTree("{\"taches\":{\"aSurveiller\":[{\"id\":100},{\"id\":101},{\"code\":\"sans-id\"}]}}");
        contexte = new ContexteProjetIa(contenu, new HashSet<>(Arrays.asList(10L, 11L)), new HashSet<Long>(),
                Collections.<String>emptyList());

        lenient().when(risqueRepository.findById(ID_RISQUE)).thenReturn(Optional.of(risque));
        lenient().when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Collections.<IaRecommandation>emptyList());
        lenient().when(contexteProjetService.construire(ID_PROJET)).thenReturn(contexte);
        lenient().when(ollamaClient.getModele()).thenReturn("qwen3:8b");
        lenient().when(iaAnalyseRepository.save(any(IaAnalyse.class))).thenAnswer(invocation -> {
            IaAnalyse analyse = invocation.getArgument(0);
            analyse.setId(100L);
            return analyse;
        });
        lenient().when(iaRecommandationRepository.save(any(IaRecommandation.class))).thenAnswer(invocation -> {
            IaRecommandation recommandation = invocation.getArgument(0);
            recommandation.setId(200L + recommandation.getLibelle().length());
            return recommandation;
        });
    }

    private void modeleRepond(SolutionProposee... solutions) {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn(REPONSE_BRUTE);
        when(reponseSuggestionRisqueService.interpreter(eq(REPONSE_BRUTE), any(Set.class), any(Set.class)))
                .thenReturn(new ReponseSuggestionRisque(Arrays.asList(solutions)));
    }

    private SolutionProposee reaffectation() {
        return new SolutionProposee("Confier T-101 à Lala", "Elle est disponible", "2 jours de retard évités",
                TypeActionRecommandation.REASSIGNER_TACHE, 100L, 11L, null, null, null);
    }

    private SolutionProposee creation() {
        return new SolutionProposee("Planifier une revue", "Détecter les blocages", null,
                TypeActionRecommandation.CREER_TACHE, null, 10L, "Organiser la revue", "Revue hebdo", 3.5);
    }

    @Test
    void suggerer_sansSolutionEnAttente_interrogeLeModeleEtEnregistreLesSolutions() {
        Utilisateur demandeur = new Utilisateur();
        demandeur.setId(ID_DEMANDEUR);
        when(utilisateurRepository.findById(ID_DEMANDEUR)).thenReturn(Optional.of(demandeur));
        modeleRepond(reaffectation(), creation());

        List<SolutionRisqueResponse> resultat = service.suggerer(ID_PROJET, ID_RISQUE, ID_DEMANDEUR);

        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).getLibelle()).isEqualTo("Confier T-101 à Lala");
        assertThat(resultat.get(0).getTypeAction()).isEqualTo("REASSIGNER_TACHE");
        assertThat(resultat.get(0).getGain()).isEqualTo("2 jours de retard évités");
        assertThat(resultat.get(0).getJustification()).isEqualTo("Elle est disponible");
        assertThat(resultat.get(1).getTypeAction()).isEqualTo("CREER_TACHE");
        assertThat(resultat.get(1).getGain()).isNull();

        ArgumentCaptor<IaAnalyse> analyse = ArgumentCaptor.forClass(IaAnalyse.class);
        verify(iaAnalyseRepository).save(analyse.capture());
        assertThat(analyse.getValue().getTypeAnalyse()).isEqualTo(TypeAnalyseIa.SUGGESTION_RISQUE);
        assertThat(analyse.getValue().getDemandeur()).isSameAs(demandeur);
        assertThat(analyse.getValue().getReponse()).isEqualTo(REPONSE_BRUTE);
        assertThat(analyse.getValue().getQuestion()).contains("Tâches bloquées");
        assertThat(analyse.getValue().getModele()).isEqualTo("qwen3:8b");

        ArgumentCaptor<IaRecommandation> recommandations = ArgumentCaptor.forClass(IaRecommandation.class);
        verify(iaRecommandationRepository, times(2)).save(recommandations.capture());
        IaRecommandation premiere = recommandations.getAllValues().get(0);
        assertThat(premiere.getRisque()).isSameAs(risque);
        assertThat(premiere.getAnalyse().getId()).isEqualTo(100L);
        assertThat(premiere.getStatut()).isEqualTo(StatutRecommandation.PROPOSEE);
        JsonNode action = premiere.getActionProposee();
        assertThat(action.get("type").asText()).isEqualTo("REASSIGNER_TACHE");
        assertThat(action.get("idTache").asLong()).isEqualTo(100L);
        assertThat(action.get("idAssigne").asLong()).isEqualTo(11L);
        assertThat(action.get("gain").asText()).isEqualTo("2 jours de retard évités");
        JsonNode actionCreation = recommandations.getAllValues().get(1).getActionProposee();
        assertThat(actionCreation.get("type").asText()).isEqualTo("CREER_TACHE");
        assertThat(actionCreation.get("titre").asText()).isEqualTo("Organiser la revue");
        assertThat(actionCreation.get("description").asText()).isEqualTo("Revue hebdo");
        assertThat(actionCreation.get("tempsEstime").asDouble()).isEqualTo(3.5);
        assertThat(actionCreation.get("idAssigne").asLong()).isEqualTo(10L);
        assertThat(actionCreation.has("gain")).isFalse();
    }

    @Test
    void suggerer_transmetAuParseurLesMembresEtLesTachesVuesParLeModele() {
        modeleRepond(creation());

        service.suggerer(ID_PROJET, ID_RISQUE, null);

        ArgumentCaptor<Set> membres = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set> taches = ArgumentCaptor.forClass(Set.class);
        verify(reponseSuggestionRisqueService).interpreter(eq(REPONSE_BRUTE), membres.capture(), taches.capture());
        assertThat(membres.getValue()).containsExactlyInAnyOrder(10L, 11L);
        assertThat(taches.getValue()).containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void suggerer_solutionsDejaEnAttente_lesRenvoieSansInterrogerLeModele() {
        IaRecommandation enAttente = recommandation(300L, StatutRecommandation.PROPOSEE, "Solution en cache");
        IaRecommandation dejaAppliquee = recommandation(301L, StatutRecommandation.APPLIQUEE, "Solution appliquée");
        when(iaRecommandationRepository.findByRisqueIdOrderByIdAsc(ID_RISQUE)).thenReturn(Arrays.asList(enAttente, dejaAppliquee));

        List<SolutionRisqueResponse> resultat = service.suggerer(ID_PROJET, ID_RISQUE, ID_DEMANDEUR);

        assertThat(resultat).extracting(SolutionRisqueResponse::getId).containsExactly(300L);
        verifyNoInteractions(ollamaClient, contexteProjetService);
        verify(iaAnalyseRepository, never()).save(any(IaAnalyse.class));
    }

    @Test
    void suggerer_risqueInconnuOuDUnAutreProjet_leveRisqueIntrouvableException() {
        Projet autreProjet = new Projet();
        autreProjet.setId(2L);
        risque.setProjet(autreProjet);
        when(risqueRepository.findById(99L)).thenReturn(Optional.<Risque>empty());

        assertThatThrownBy(() -> service.suggerer(ID_PROJET, 99L, null)).isInstanceOf(RisqueIntrouvableException.class);
        assertThatThrownBy(() -> service.suggerer(ID_PROJET, ID_RISQUE, null)).isInstanceOf(RisqueIntrouvableException.class);

        verifyNoInteractions(ollamaClient);
    }

    @Test
    void suggerer_risqueNonOuvert_leveStatutRisqueInvalideException() {
        for (StatutRisque statut : new StatutRisque[]{StatutRisque.SOUS_CONTROLE, StatutRisque.REJETE, StatutRisque.CLOS}) {
            risque.setStatut(statut);
            assertThatThrownBy(() -> service.suggerer(ID_PROJET, ID_RISQUE, null))
                    .isInstanceOf(StatutRisqueInvalideException.class);
        }

        verifyNoInteractions(ollamaClient);
    }

    @Test
    void suggerer_risqueRejetePendantLAppelAuModele_neRienEnregistre() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenAnswer(invocation -> {
            risque.setStatut(StatutRisque.REJETE);
            return REPONSE_BRUTE;
        });
        when(reponseSuggestionRisqueService.interpreter(eq(REPONSE_BRUTE), any(Set.class), any(Set.class)))
                .thenReturn(new ReponseSuggestionRisque(Collections.singletonList(creation())));

        assertThatThrownBy(() -> service.suggerer(ID_PROJET, ID_RISQUE, null))
                .isInstanceOf(StatutRisqueInvalideException.class);

        verify(iaAnalyseRepository, never()).save(any(IaAnalyse.class));
        verify(iaRecommandationRepository, never()).save(any(IaRecommandation.class));
    }

    @Test
    void suggerer_modeleIndisponible_neRienEnregistreEtLibereLeVerrou() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap()))
                .thenThrow(new OllamaIndisponibleException("indisponible"))
                .thenReturn(REPONSE_BRUTE);
        when(reponseSuggestionRisqueService.interpreter(eq(REPONSE_BRUTE), any(Set.class), any(Set.class)))
                .thenReturn(new ReponseSuggestionRisque(Collections.singletonList(creation())));

        assertThatThrownBy(() -> service.suggerer(ID_PROJET, ID_RISQUE, null)).isInstanceOf(OllamaIndisponibleException.class);
        verify(iaRecommandationRepository, never()).save(any(IaRecommandation.class));

        assertThat(service.suggerer(ID_PROJET, ID_RISQUE, null)).hasSize(1);
    }

    @Test
    void suggerer_rechercheDejaEnCoursPourLeMemeRisque_leveSuggestionDejaEnCoursException() {
        AtomicReference<Throwable> erreurDeLaSecondeDemande = new AtomicReference<>();
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenAnswer(invocation -> {
            try {
                service.suggerer(ID_PROJET, ID_RISQUE, null);
            } catch (RuntimeException e) {
                erreurDeLaSecondeDemande.set(e);
            }
            return REPONSE_BRUTE;
        });
        when(reponseSuggestionRisqueService.interpreter(eq(REPONSE_BRUTE), any(Set.class), any(Set.class)))
                .thenReturn(new ReponseSuggestionRisque(Collections.singletonList(creation())));

        service.suggerer(ID_PROJET, ID_RISQUE, null);

        assertThat(erreurDeLaSecondeDemande.get()).isInstanceOf(SuggestionDejaEnCoursException.class);
        verify(iaAnalyseRepository, times(1)).save(any(IaAnalyse.class));
    }

    private IaRecommandation recommandation(Long id, StatutRecommandation statut, String libelle) {
        IaRecommandation recommandation = new IaRecommandation();
        recommandation.setId(id);
        recommandation.setStatut(statut);
        recommandation.setLibelle(libelle);
        recommandation.setActionProposee(objectMapper.createObjectNode().put("type", "CREER_TACHE").put("gain", "1 jour"));
        return recommandation;
    }
}
