package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque.ReevaluationRisque;
import mg.gov.mef.dsi.gpdi.dto.response.ReponseDetectionRisque.RisqueDetecte;
import mg.gov.mef.dsi.gpdi.dto.response.ResultatDetectionRisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AnalyseDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import mg.gov.mef.dsi.gpdi.service.ReponseDetectionRisqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionRisqueServiceImplTest {

    private static final Long ID_PROJET = 1L;
    private static final Long ID_DEMANDEUR = 50L;
    private static final String REPONSE_BRUTE = "{\"brut\":true}";

    @Mock
    private ContexteProjetService contexteProjetService;
    @Mock
    private ReponseDetectionRisqueService reponseDetectionRisqueService;
    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private RisqueRepository risqueRepository;
    @Mock
    private IaAnalyseRepository iaAnalyseRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private DetectionRisqueServiceImpl service;
    private Projet projet;
    private Utilisateur responsable;
    private ContexteProjetIa contexte;

    @BeforeEach
    void setUp() {
        service = new DetectionRisqueServiceImpl(contexteProjetService, reponseDetectionRisqueService, ollamaClient,
                projetRepository, utilisateurRepository, risqueRepository, iaAnalyseRepository,
                new TransactionTemplate(transactionManager));

        responsable = utilisateur(1L);
        projet = new Projet();
        projet.setId(ID_PROJET);
        projet.setResponsable(responsable);
        contexte = new ContexteProjetIa(new ObjectMapper().createObjectNode(), new HashSet<Long>(),
                new HashSet<Long>(), Collections.<String>emptyList());

        lenient().when(projetRepository.existsById(ID_PROJET)).thenReturn(true);
        lenient().when(projetRepository.findById(ID_PROJET)).thenReturn(Optional.of(projet));
        lenient().when(contexteProjetService.construire(ID_PROJET)).thenReturn(contexte);
        lenient().when(ollamaClient.getModele()).thenReturn("qwen3:8b");
        lenient().when(risqueRepository.findByProjetIdOrderByIdAsc(ID_PROJET)).thenReturn(Collections.<Risque>emptyList());
        lenient().when(iaAnalyseRepository.save(any(IaAnalyse.class))).thenAnswer(invocation -> {
            IaAnalyse analyse = invocation.getArgument(0);
            analyse.setId(100L);
            return analyse;
        });
    }

    @Test
    void detecter_nouveauxRisques_lesCreeAvecNiveauCalculeEtPorteur() {
        Utilisateur porteurIa = utilisateur(2L);
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(porteurIa));
        when(utilisateurRepository.findById(ID_DEMANDEUR)).thenReturn(Optional.of(utilisateur(ID_DEMANDEUR)));
        modeleRepond(new ReponseDetectionRisque(Arrays.asList(
                new RisqueDetecte("Tâches bloquées", "desc 1", 5, 5, "parade 1", 2L),
                new RisqueDetecte("Surcharge", "desc 2", 2, 2, "parade 2", null)),
                Collections.<ReevaluationRisque>emptyList(), "Synthese", new BigDecimal("0.650")));

        ResultatDetectionRisqueResponse resultat = service.detecter(ID_PROJET, ID_DEMANDEUR);

        assertThat(resultat.getIdAnalyse()).isEqualTo(100L);
        assertThat(resultat.getNombreNouveauxRisques()).isEqualTo(2);
        assertThat(resultat.getNombreRisquesReevalues()).isZero();
        assertThat(resultat.getSynthese()).isEqualTo("Synthese");

        ArgumentCaptor<Risque> captor = ArgumentCaptor.forClass(Risque.class);
        verify(risqueRepository, times(2)).save(captor.capture());
        Risque critique = captor.getAllValues().get(0);
        assertThat(critique.getLibelle()).isEqualTo("Tâches bloquées");
        assertThat(critique.getNiveau()).isEqualTo(NiveauRisque.CRITIQUE);
        assertThat(critique.getStatut()).isEqualTo(StatutRisque.OUVERT);
        assertThat(critique.getMesureMitigation()).isEqualTo("parade 1");
        assertThat(critique.getPorteur()).isSameAs(porteurIa);
        assertThat(critique.getProjet()).isSameAs(projet);
        assertThat(critique.getAnalyse().getId()).isEqualTo(100L);
        Risque faible = captor.getAllValues().get(1);
        assertThat(faible.getNiveau()).isEqualTo(NiveauRisque.FAIBLE);
        assertThat(faible.getPorteur()).isSameAs(responsable);
    }

    @Test
    void detecter_tracelAnalyseAvecDemandeurContexteEtModele() {
        Utilisateur demandeur = utilisateur(ID_DEMANDEUR);
        when(utilisateurRepository.findById(ID_DEMANDEUR)).thenReturn(Optional.of(demandeur));
        modeleRepond(new ReponseDetectionRisque(Collections.<RisqueDetecte>emptyList(),
                Collections.<ReevaluationRisque>emptyList(), "RAS", new BigDecimal("0.800")));

        service.detecter(ID_PROJET, ID_DEMANDEUR);

        ArgumentCaptor<IaAnalyse> captor = ArgumentCaptor.forClass(IaAnalyse.class);
        verify(iaAnalyseRepository).save(captor.capture());
        IaAnalyse analyse = captor.getValue();
        assertThat(analyse.getTypeAnalyse()).isEqualTo(TypeAnalyseIa.DETECTION_RISQUE);
        assertThat(analyse.getProjet()).isSameAs(projet);
        assertThat(analyse.getDemandeur()).isSameAs(demandeur);
        assertThat(analyse.getReponse()).isEqualTo("RAS");
        assertThat(analyse.getContexte()).isSameAs(contexte.getContenu());
        assertThat(analyse.getIndiceConfiance()).isEqualByComparingTo(new BigDecimal("0.8"));
        assertThat(analyse.getModele()).isEqualTo("qwen3:8b");
    }

    @Test
    void detecter_analyseAutomatique_neRenseignePasDeDemandeur() {
        modeleRepond(new ReponseDetectionRisque(Collections.<RisqueDetecte>emptyList(),
                Collections.<ReevaluationRisque>emptyList(), null, null));

        service.detecter(ID_PROJET, null);

        ArgumentCaptor<IaAnalyse> captor = ArgumentCaptor.forClass(IaAnalyse.class);
        verify(iaAnalyseRepository).save(captor.capture());
        assertThat(captor.getValue().getDemandeur()).isNull();
        verify(utilisateurRepository, never()).findById(any(Long.class));
    }

    @Test
    void detecter_reevaluations_closeLesRisquesRedevenusFaiblesEtIgnoreLesAutresStatuts() {
        Risque redevenuFaible = risque(10L, StatutRisque.SOUS_CONTROLE, 4, 4);
        Risque resteSousControle = risque(11L, StatutRisque.SOUS_CONTROLE, 4, 4);
        Risque rejeteEntreTemps = risque(12L, StatutRisque.REJETE, 4, 4);
        when(risqueRepository.findByProjetIdOrderByIdAsc(ID_PROJET))
                .thenReturn(Arrays.asList(redevenuFaible, resteSousControle, rejeteEntreTemps));
        modeleRepond(new ReponseDetectionRisque(Collections.<RisqueDetecte>emptyList(), Arrays.asList(
                new ReevaluationRisque(10L, 2, 2),
                new ReevaluationRisque(11L, 3, 3),
                new ReevaluationRisque(12L, 1, 1),
                new ReevaluationRisque(99L, 1, 1)), "Synthese", null));

        ResultatDetectionRisqueResponse resultat = service.detecter(ID_PROJET, null);

        assertThat(resultat.getNombreRisquesReevalues()).isEqualTo(2);
        assertThat(resultat.getNombreRisquesClos()).isEqualTo(1);
        assertThat(redevenuFaible.getStatut()).isEqualTo(StatutRisque.CLOS);
        assertThat(redevenuFaible.getNiveau()).isEqualTo(NiveauRisque.FAIBLE);
        assertThat(resteSousControle.getStatut()).isEqualTo(StatutRisque.SOUS_CONTROLE);
        assertThat(resteSousControle.getNiveau()).isEqualTo(NiveauRisque.MOYEN);
        assertThat(resteSousControle.getProbabilite()).isEqualTo(3);
        assertThat(rejeteEntreTemps.getStatut()).isEqualTo(StatutRisque.REJETE);
        assertThat(rejeteEntreTemps.getProbabilite()).isEqualTo(4);
        verify(risqueRepository, never()).save(rejeteEntreTemps);
    }

    @Test
    void detecter_projetInconnu_leveProjetIntrouvableSansAppelerLeModele() {
        when(projetRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.detecter(99L, null)).isInstanceOf(ProjetIntrouvableException.class);

        verifyNoInteractions(ollamaClient, contexteProjetService);
    }

    @Test
    void detecter_modeleIndisponible_neRienEnregistreEtLibereLeVerrou() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap()))
                .thenThrow(new OllamaIndisponibleException("indisponible"))
                .thenReturn(REPONSE_BRUTE);
        when(reponseDetectionRisqueService.interpreter(REPONSE_BRUTE, contexte)).thenReturn(new ReponseDetectionRisque(
                Collections.<RisqueDetecte>emptyList(), Collections.<ReevaluationRisque>emptyList(), "ok", null));

        assertThatThrownBy(() -> service.detecter(ID_PROJET, null)).isInstanceOf(OllamaIndisponibleException.class);
        verify(iaAnalyseRepository, never()).save(any(IaAnalyse.class));

        assertThat(service.detecter(ID_PROJET, null).getIdAnalyse()).isEqualTo(100L);
    }

    @Test
    void detecter_analyseDejaEnCoursSurLeMemeProjet_leveAnalyseDejaEnCoursException() {
        AtomicReference<Throwable> erreurDeLaSecondeDemande = new AtomicReference<>();
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenAnswer(invocation -> {
            try {
                service.detecter(ID_PROJET, null);
            } catch (RuntimeException e) {
                erreurDeLaSecondeDemande.set(e);
            }
            return REPONSE_BRUTE;
        });
        when(reponseDetectionRisqueService.interpreter(REPONSE_BRUTE, contexte)).thenReturn(new ReponseDetectionRisque(
                Collections.<RisqueDetecte>emptyList(), Collections.<ReevaluationRisque>emptyList(), "ok", null));

        service.detecter(ID_PROJET, null);

        assertThat(erreurDeLaSecondeDemande.get()).isInstanceOf(AnalyseDejaEnCoursException.class);
        verify(iaAnalyseRepository, times(1)).save(any(IaAnalyse.class));
    }

    private void modeleRepond(ReponseDetectionRisque reponse) {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn(REPONSE_BRUTE);
        when(reponseDetectionRisqueService.interpreter(REPONSE_BRUTE, contexte)).thenReturn(reponse);
    }

    private Utilisateur utilisateur(Long id) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        return utilisateur;
    }

    private Risque risque(Long id, StatutRisque statut, int probabilite, int impact) {
        Risque risque = new Risque();
        risque.setId(id);
        risque.setStatut(statut);
        risque.setProbabilite(probabilite);
        risque.setImpact(impact);
        risque.setNiveau(NiveauRisque.calculer(probabilite, impact));
        return risque;
    }
}
