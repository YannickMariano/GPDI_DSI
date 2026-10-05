package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.RecommandationProjetResponse;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseRecommandationProjet.RecommandationProposee;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.entity.AffectationTache;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.Priorite;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutTache;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.TypeActionRecommandationProjet;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesRecommandationsProjetRefuseException;
import mg.gov.mef.dsi.gpdi.exception.AnalyseRecommandationsDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.RecommandationProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.StatutRecommandationProjetInvalideException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonMembreEquipeException;
import mg.gov.mef.dsi.gpdi.mapper.RecommandationProjetMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.service.ReponseRecommandationProjetService;
import mg.gov.mef.dsi.gpdi.service.TacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommandationProjetServiceImplTest {

    private static final Long ID_PROJET = 1L;
    private static final Long ID_RESPONSABLE = 10L;
    private static final Long ID_AUTRE = 20L;
    private static final Long ID_EQUIPE = 3L;
    private static final Long ID_MEMBRE = 11L;
    private static final Long ID_TACHE = 100L;
    private static final Long ID_RECOMMANDATION = 50L;
    private static final String REPONSE_BRUTE = "{\"brut\":true}";

    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private IaAnalyseRepository iaAnalyseRepository;
    @Mock
    private IaRecommandationRepository iaRecommandationRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private AffectationTacheRepository affectationTacheRepository;
    @Mock
    private MembreEquipeRepository membreEquipeRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourantService;
    @Mock
    private ContexteProjetService contexteProjetService;
    @Mock
    private ReponseRecommandationProjetService reponseRecommandationProjetService;
    @Mock
    private TacheService tacheService;
    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RecommandationProjetServiceImpl service;
    private Projet projet;
    private Utilisateur responsable;
    private Utilisateur autre;
    private ContexteProjetIa contexte;

    @BeforeEach
    void setUp() throws Exception {
        service = new RecommandationProjetServiceImpl(projetRepository, iaAnalyseRepository, iaRecommandationRepository,
                tacheRepository, affectationTacheRepository, membreEquipeRepository, utilisateurRepository,
                utilisateurCourantService, contexteProjetService, reponseRecommandationProjetService,
                Mappers.getMapper(RecommandationProjetMapper.class), tacheService, ollamaClient, objectMapper,
                new TransactionTemplate(transactionManager));

        responsable = new Utilisateur();
        responsable.setId(ID_RESPONSABLE);
        autre = new Utilisateur();
        autre.setId(ID_AUTRE);

        Equipe equipe = new Equipe();
        equipe.setId(ID_EQUIPE);
        projet = new Projet();
        projet.setId(ID_PROJET);
        projet.setResponsable(responsable);
        projet.setEquipe(equipe);

        JsonNode contenu = objectMapper.readTree("{\"taches\":{\"aSurveiller\":[{\"id\":100}]}}");
        contexte = new ContexteProjetIa(contenu, new HashSet<>(Arrays.asList(ID_MEMBRE)), Collections.<Long>emptySet(), Collections.<String>emptyList());

        lenient().when(projetRepository.findById(ID_PROJET)).thenReturn(Optional.of(projet));
        lenient().when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(responsable);
        lenient().when(contexteProjetService.construire(ID_PROJET)).thenReturn(contexte);
        lenient().when(ollamaClient.getModele()).thenReturn("qwen3:8b");
        lenient().when(iaAnalyseRepository.save(any(IaAnalyse.class))).thenAnswer(invocation -> {
            IaAnalyse analyse = invocation.getArgument(0);
            analyse.setId(200L);
            return analyse;
        });
        lenient().when(iaRecommandationRepository.save(any(IaRecommandation.class))).thenAnswer(invocation -> {
            IaRecommandation recommandation = invocation.getArgument(0);
            if (recommandation.getId() == null) {
                recommandation.setId(ID_RECOMMANDATION);
            }
            return recommandation;
        });
        lenient().when(utilisateurRepository.findById(ID_RESPONSABLE)).thenReturn(Optional.of(responsable));
        lenient().when(iaRecommandationRepository.findByAnalyseProjetIdAndRisqueIsNullAndStatutOrderByIdDesc(ID_PROJET, StatutRecommandation.PROPOSEE))
                .thenReturn(Collections.<IaRecommandation>emptyList());
    }

    @Test
    void analyser_chefDeProjet_enregistreUneAnalyseEtLesRecommandations() {
        when(reponseRecommandationProjetService.interpreter(eq(REPONSE_BRUTE), any(), any(), any())).thenReturn(
                new ReponseRecommandationProjet(Arrays.asList(
                        new RecommandationProposee(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, "Retirer T-100 du sprint",
                                "Sprint en surcapacite", "Sprint 4 : 41h/35h", ID_TACHE, null, null))));
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn(REPONSE_BRUTE);

        List<RecommandationProjetResponse> resultat = service.analyser(ID_PROJET, ID_RESPONSABLE);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getType()).isEqualTo("RETIRER_DU_SPRINT");
        assertThat(resultat.get(0).getLibelle()).isEqualTo("Retirer T-100 du sprint");
        assertThat(resultat.get(0).getPreuve()).isEqualTo("Sprint 4 : 41h/35h");
        assertThat(resultat.get(0).getStatut()).isEqualTo("PROPOSEE");

        ArgumentCaptor<IaAnalyse> captor = ArgumentCaptor.forClass(IaAnalyse.class);
        verify(iaAnalyseRepository).save(captor.capture());
        assertThat(captor.getValue().getProjet()).isSameAs(projet);
        assertThat(captor.getValue().getDemandeur()).isSameAs(responsable);
    }

    @Test
    void analyser_aucuneRecommandationJustifiee_renvoieUneListeVide() {
        when(reponseRecommandationProjetService.interpreter(eq(REPONSE_BRUTE), any(), any(), any()))
                .thenReturn(new ReponseRecommandationProjet(Collections.<RecommandationProposee>emptyList()));
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn(REPONSE_BRUTE);

        assertThat(service.analyser(ID_PROJET, ID_RESPONSABLE)).isEmpty();
        verify(iaAnalyseRepository).save(any(IaAnalyse.class));
    }

    @Test
    void analyser_utilisateurNonResponsable_leveAccesRefuseSansAppelerOllama() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);

        assertThatThrownBy(() -> service.analyser(ID_PROJET, ID_AUTRE))
                .isInstanceOf(AccesRecommandationsProjetRefuseException.class);

        verifyNoInteractions(ollamaClient);
    }

    @Test
    void analyser_projetInconnu_leveProjetIntrouvableException() {
        when(projetRepository.findById(99L)).thenReturn(Optional.<Projet>empty());

        assertThatThrownBy(() -> service.analyser(99L, ID_RESPONSABLE)).isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void analyser_analyseDejaEnCoursSurLeMemeProjet_leveAnalyseRecommandationsDejaEnCoursException() {
        AtomicReference<Throwable> erreurSeconde = new AtomicReference<>();
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenAnswer(invocation -> {
            try {
                service.analyser(ID_PROJET, ID_RESPONSABLE);
            } catch (RuntimeException e) {
                erreurSeconde.set(e);
            }
            return REPONSE_BRUTE;
        });
        when(reponseRecommandationProjetService.interpreter(eq(REPONSE_BRUTE), any(), any(), any()))
                .thenReturn(new ReponseRecommandationProjet(Collections.<RecommandationProposee>emptyList()));

        service.analyser(ID_PROJET, ID_RESPONSABLE);

        assertThat(erreurSeconde.get()).isInstanceOf(AnalyseRecommandationsDejaEnCoursException.class);
    }

    @Test
    void lister_chefDeProjet_retourneLesRecommandationsEnAttente() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, null, null));
        when(iaRecommandationRepository.findByAnalyseProjetIdAndRisqueIsNullAndStatutOrderByIdDesc(ID_PROJET, StatutRecommandation.PROPOSEE))
                .thenReturn(Collections.singletonList(recommandation));

        assertThat(service.lister(ID_PROJET)).hasSize(1);
    }

    @Test
    void lister_utilisateurNonResponsable_leveAccesRecommandationsProjetRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);

        assertThatThrownBy(() -> service.lister(ID_PROJET)).isInstanceOf(AccesRecommandationsProjetRefuseException.class);
    }

    @Test
    void lister_directeurDsi_peutConsulterMemeSansEtreLeChefDeProjet() {
        Role role = new Role();
        role.setNom("directeur_dsi");
        autre.setRole(role);
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, null, null));
        when(iaRecommandationRepository.findByAnalyseProjetIdAndRisqueIsNullAndStatutOrderByIdDesc(ID_PROJET, StatutRecommandation.PROPOSEE))
                .thenReturn(Collections.singletonList(recommandation));

        assertThat(service.lister(ID_PROJET)).hasSize(1);
    }

    @Test
    void analyser_directeurDsi_resteInterditCarPasChefDeProjet() {
        Role role = new Role();
        role.setNom("directeur_dsi");
        autre.setRole(role);
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);

        assertThatThrownBy(() -> service.analyser(ID_PROJET, ID_AUTRE)).isInstanceOf(AccesRecommandationsProjetRefuseException.class);

        verifyNoInteractions(ollamaClient);
    }

    @Test
    void appliquer_reaffectation_reaffecteLaTacheEtMarqueApplique() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.REASSIGNER_TACHE, ID_MEMBRE, null));
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));
        when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(ID_EQUIPE, ID_MEMBRE)).thenReturn(true);
        Tache tache = tache(StatutTache.EN_COURS);
        when(tacheRepository.findById(ID_TACHE)).thenReturn(Optional.of(tache));
        when(affectationTacheRepository.findByTacheId(ID_TACHE)).thenReturn(Collections.<AffectationTache>emptyList());
        when(tacheService.modifier(eq(ID_PROJET), eq(ID_TACHE), any(TacheRequest.class))).thenReturn(reponseTache("T-100"));

        RecommandationProjetResponse resultat = service.appliquer(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE);

        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).modifier(eq(ID_PROJET), eq(ID_TACHE), requete.capture());
        assertThat(requete.getValue().getIdsUtilisateursAssignes()).containsExactly(ID_MEMBRE);
        assertThat(requete.getValue().getTitre()).isEqualTo("Migration");
        assertThat(resultat.getStatut()).isEqualTo("APPLIQUEE");
        assertThat(resultat.getResultat()).contains("T-100");
        assertThat(recommandation.getStatut()).isEqualTo(StatutRecommandation.APPLIQUEE);
        assertThat(recommandation.getValidateur()).isSameAs(responsable);
    }

    @Test
    void appliquer_reaffectationVersMembreHorsEquipe_leveUtilisateurNonMembreEquipeExceptionSansModifierLaTache() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.REASSIGNER_TACHE, 999L, null));
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));
        when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(ID_EQUIPE, 999L)).thenReturn(false);

        assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE))
                .isInstanceOf(UtilisateurNonMembreEquipeException.class);

        verifyNoInteractions(tacheService);
        assertThat(recommandation.getStatut()).isEqualTo(StatutRecommandation.PROPOSEE);
    }

    @Test
    void appliquer_retirerDuSprint_metIdSprintANullEtConserveLesAutresChamps() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, null, null));
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));
        Tache tache = tache(StatutTache.EN_COURS);
        when(tacheRepository.findById(ID_TACHE)).thenReturn(Optional.of(tache));
        when(affectationTacheRepository.findByTacheId(ID_TACHE)).thenReturn(Collections.singletonList(affectation(ID_MEMBRE)));
        when(tacheService.modifier(eq(ID_PROJET), eq(ID_TACHE), any(TacheRequest.class))).thenReturn(reponseTache("T-100"));

        RecommandationProjetResponse resultat = service.appliquer(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE);

        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).modifier(eq(ID_PROJET), eq(ID_TACHE), requete.capture());
        assertThat(requete.getValue().getIdSprint()).isNull();
        assertThat(requete.getValue().getIdsUtilisateursAssignes()).containsExactly(ID_MEMBRE);
        assertThat(resultat.getResultat()).contains("retirée du sprint");
    }

    @Test
    void appliquer_ajusterEstimation_metATempsEstimeLaNouvelleValeur() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.AJUSTER_ESTIMATION, null, 12.5));
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));
        Tache tache = tache(StatutTache.EN_COURS);
        when(tacheRepository.findById(ID_TACHE)).thenReturn(Optional.of(tache));
        when(affectationTacheRepository.findByTacheId(ID_TACHE)).thenReturn(Collections.<AffectationTache>emptyList());
        when(tacheService.modifier(eq(ID_PROJET), eq(ID_TACHE), any(TacheRequest.class))).thenReturn(reponseTache("T-100"));

        RecommandationProjetResponse resultat = service.appliquer(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE);

        ArgumentCaptor<TacheRequest> requete = ArgumentCaptor.forClass(TacheRequest.class);
        verify(tacheService).modifier(eq(ID_PROJET), eq(ID_TACHE), requete.capture());
        assertThat(requete.getValue().getTempsEstime()).isEqualTo(12.5);
        assertThat(resultat.getResultat()).contains("12.5");
    }

    @Test
    void appliquer_tacheCloturee_leveTacheIntrouvableException() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, null, null));
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));
        when(tacheRepository.findById(ID_TACHE)).thenReturn(Optional.of(tache(StatutTache.TERMINEE)));

        assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE))
                .isInstanceOf(TacheIntrouvableException.class);
    }

    @Test
    void appliquer_recommandationDejaTraitee_leveStatutRecommandationProjetInvalideException() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, null, null));
        recommandation.setStatut(StatutRecommandation.APPLIQUEE);
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));

        assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE))
                .isInstanceOf(StatutRecommandationProjetInvalideException.class);

        verifyNoInteractions(tacheService);
    }

    @Test
    void appliquer_recommandationDunAutreProjet_leveRecommandationProjetIntrouvableException() {
        Projet autreProjet = new Projet();
        autreProjet.setId(999L);
        IaAnalyse analyseAutreProjet = new IaAnalyse();
        analyseAutreProjet.setProjet(autreProjet);
        IaRecommandation recommandation = new IaRecommandation();
        recommandation.setId(ID_RECOMMANDATION);
        recommandation.setAnalyse(analyseAutreProjet);
        recommandation.setStatut(StatutRecommandation.PROPOSEE);
        recommandation.setActionProposee(action(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, null, null));
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));

        assertThatThrownBy(() -> service.appliquer(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE))
                .isInstanceOf(RecommandationProjetIntrouvableException.class);
    }

    @Test
    void rejeter_recommandationEnAttente_laRejetteSansExecuterAucuneAction() {
        IaRecommandation recommandation = recommandation(ID_RECOMMANDATION, action(TypeActionRecommandationProjet.RETIRER_DU_SPRINT, null, null));
        when(iaRecommandationRepository.findById(ID_RECOMMANDATION)).thenReturn(Optional.of(recommandation));

        RecommandationProjetResponse resultat = service.rejeter(ID_PROJET, ID_RECOMMANDATION, ID_RESPONSABLE);

        assertThat(resultat.getStatut()).isEqualTo("REJETEE");
        assertThat(recommandation.getValidateur()).isSameAs(responsable);
        verifyNoInteractions(tacheService);
    }

    private IaRecommandation recommandation(Long id, JsonNode action) {
        IaAnalyse analyse = new IaAnalyse();
        analyse.setProjet(projet);
        IaRecommandation recommandation = new IaRecommandation();
        recommandation.setId(id);
        recommandation.setAnalyse(analyse);
        recommandation.setLibelle("Recommandation " + id);
        recommandation.setStatut(StatutRecommandation.PROPOSEE);
        recommandation.setActionProposee(action);
        return recommandation;
    }

    private JsonNode action(TypeActionRecommandationProjet type, Long idAssigne, Double nouvelleEstimation) {
        com.fasterxml.jackson.databind.node.ObjectNode noeud = objectMapper.createObjectNode();
        noeud.put("type", type.name());
        noeud.put("idTache", ID_TACHE);
        if (idAssigne != null) {
            noeud.put("idAssigne", idAssigne);
        }
        if (nouvelleEstimation != null) {
            noeud.put("nouvelleEstimation", nouvelleEstimation);
        }
        return noeud;
    }

    private AffectationTache affectation(Long idUtilisateur) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(idUtilisateur);
        AffectationTache affectation = new AffectationTache();
        affectation.setUtilisateur(utilisateur);
        return affectation;
    }

    private Tache tache(StatutTache statut) {
        Tache tache = new Tache();
        tache.setId(ID_TACHE);
        tache.setProjet(projet);
        tache.setTitre("Migration");
        tache.setDescription("Description");
        tache.setPriorite(Priorite.HAUTE);
        tache.setTempsEstime(40.0);
        tache.setStatut(statut);
        return tache;
    }

    private TacheResponse reponseTache(String code) {
        TacheResponse reponse = new TacheResponse();
        reponse.setId(ID_TACHE);
        reponse.setCode(code);
        return reponse;
    }
}
