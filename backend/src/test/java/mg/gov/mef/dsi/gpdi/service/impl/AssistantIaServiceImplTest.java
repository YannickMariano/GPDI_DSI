package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.client.OllamaClient;
import mg.gov.mef.dsi.gpdi.dto.response.ContexteProjetIa;
import mg.gov.mef.dsi.gpdi.dto.response.MessageAssistantResponse;
import mg.gov.mef.dsi.gpdi.entity.IaAnalyse;
import mg.gov.mef.dsi.gpdi.entity.PorteeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.TypeAnalyseIa;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesAssistantIaRefuseException;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.repository.IaAnalyseRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.ContexteProjetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantIaServiceImplTest {

    private static final Long ID_PROJET = 1L;
    private static final Long ID_RESPONSABLE = 10L;
    private static final Long ID_AUTRE = 20L;

    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private IaAnalyseRepository iaAnalyseRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ContexteProjetService contexteProjetService;
    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private UtilisateurCourantService utilisateurCourantService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AssistantIaServiceImpl service;
    private Projet projet;
    private Utilisateur responsable;
    private Utilisateur autre;
    private ContexteProjetIa contexte;

    @BeforeEach
    void setUp() throws Exception {
        service = new AssistantIaServiceImpl(projetRepository, iaAnalyseRepository, utilisateurRepository,
                contexteProjetService, ollamaClient, utilisateurCourantService, objectMapper, new TransactionTemplate(transactionManager));

        responsable = new Utilisateur();
        responsable.setId(ID_RESPONSABLE);
        autre = new Utilisateur();
        autre.setId(ID_AUTRE);

        projet = new Projet();
        projet.setId(ID_PROJET);
        projet.setResponsable(responsable);

        JsonNode contenu = objectMapper.readTree("{\"avancement\":42}");
        contexte = new ContexteProjetIa(contenu, Collections.<Long>emptySet(), Collections.<Long>emptySet(), Collections.<String>emptyList());

        lenient().when(projetRepository.findById(ID_PROJET)).thenReturn(Optional.of(projet));
        lenient().when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(responsable);
        lenient().when(contexteProjetService.construire(ID_PROJET)).thenReturn(contexte);
        lenient().when(ollamaClient.getModele()).thenReturn("qwen3:8b");
        lenient().when(utilisateurRepository.findById(ID_RESPONSABLE)).thenReturn(Optional.of(responsable));
        lenient().when(iaAnalyseRepository.save(any(IaAnalyse.class))).thenAnswer(invocation -> {
            IaAnalyse analyse = invocation.getArgument(0);
            analyse.setId(100L);
            return analyse;
        });
    }

    @Test
    void poserQuestion_chefDeProjet_appelleOllamaEtEnregistreLEchange() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap()))
                .thenReturn("{\"reponse\":\"3 sprints clotures, avancement de 42 %.\",\"fondement\":\"3 sprints clotures\"}");

        MessageAssistantResponse reponse = service.poserQuestion(ID_PROJET, "Où en est-on ?", ID_RESPONSABLE);

        assertThat(reponse.getId()).isEqualTo(100L);
        assertThat(reponse.getQuestion()).isEqualTo("Où en est-on ?");
        assertThat(reponse.getReponse()).isEqualTo("3 sprints clotures, avancement de 42 %.");
        assertThat(reponse.getFondement()).isEqualTo("3 sprints clotures");

        ArgumentCaptor<IaAnalyse> captor = ArgumentCaptor.forClass(IaAnalyse.class);
        verify(iaAnalyseRepository).save(captor.capture());
        assertThat(captor.getValue().getProjet()).isSameAs(projet);
        assertThat(captor.getValue().getDemandeur()).isSameAs(responsable);
        assertThat(captor.getValue().getTypeAnalyse()).isEqualTo(TypeAnalyseIa.ANALYSE_PROJET);
        assertThat(captor.getValue().getPortee()).isEqualTo(PorteeAnalyseIa.PROJET);
        assertThat(captor.getValue().getModele()).isEqualTo("qwen3:8b");
    }

    @Test
    void poserQuestion_utilisateurNonResponsableDuProjet_leveAccesAssistantIaRefuseExceptionSansAppelerOllama() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);

        assertThatThrownBy(() -> service.poserQuestion(ID_PROJET, "Question", ID_AUTRE))
                .isInstanceOf(AccesAssistantIaRefuseException.class);

        verifyNoInteractions(ollamaClient);
        verify(iaAnalyseRepository, never()).save(any(IaAnalyse.class));
    }

    @Test
    void poserQuestion_projetInconnu_leveProjetIntrouvableException() {
        when(projetRepository.findById(99L)).thenReturn(Optional.<Projet>empty());

        assertThatThrownBy(() -> service.poserQuestion(99L, "Question", ID_RESPONSABLE))
                .isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void poserQuestion_reponseSansChampReponse_leveReponseIaInvalideException() {
        when(ollamaClient.genererJson(anyString(), anyString(), anyMap())).thenReturn("{\"fondement\":\"sans reponse\"}");

        assertThatThrownBy(() -> service.poserQuestion(ID_PROJET, "Question", ID_RESPONSABLE))
                .isInstanceOf(ReponseIaInvalideException.class);

        verify(iaAnalyseRepository, never()).save(any(IaAnalyse.class));
    }

    @Test
    void historique_chefDeProjet_retourneLesEchangesDuPlusAncienAuPlusRecent() {
        IaAnalyse ancien = analyse(1L, "Ancienne question", "Ancienne reponse", LocalDateTime.of(2026, 9, 1, 8, 0));
        IaAnalyse recent = analyse(2L, "Question recente", "Reponse recente", LocalDateTime.of(2026, 9, 2, 8, 0));
        when(iaAnalyseRepository.findByProjetIdAndTypeAnalyseOrderByDateCreationAsc(ID_PROJET, TypeAnalyseIa.ANALYSE_PROJET))
                .thenReturn(Arrays.asList(ancien, recent));

        List<MessageAssistantResponse> historique = service.historique(ID_PROJET);

        assertThat(historique).hasSize(2);
        assertThat(historique.get(0).getQuestion()).isEqualTo("Ancienne question");
        assertThat(historique.get(1).getQuestion()).isEqualTo("Question recente");
        assertThat(historique.get(0).getFondement()).isNull();
    }

    @Test
    void historique_utilisateurNonResponsableDuProjet_leveAccesAssistantIaRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(autre);

        assertThatThrownBy(() -> service.historique(ID_PROJET)).isInstanceOf(AccesAssistantIaRefuseException.class);

        verifyNoInteractions(iaAnalyseRepository);
    }

    private IaAnalyse analyse(Long id, String question, String reponse, LocalDateTime dateCreation) {
        IaAnalyse analyse = new IaAnalyse();
        analyse.setId(id);
        analyse.setProjet(projet);
        analyse.setQuestion(question);
        analyse.setReponse(reponse);
        analyse.setDateCreation(dateCreation);
        return analyse;
    }
}
