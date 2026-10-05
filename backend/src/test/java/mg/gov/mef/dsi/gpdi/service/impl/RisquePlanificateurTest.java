package mg.gov.mef.dsi.gpdi.service.impl;

import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.StatutProjet;
import mg.gov.mef.dsi.gpdi.exception.AnalyseDejaEnCoursException;
import mg.gov.mef.dsi.gpdi.exception.OllamaIndisponibleException;
import mg.gov.mef.dsi.gpdi.exception.ReponseIaInvalideException;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.service.DetectionRisqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RisquePlanificateurTest {

    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private DetectionRisqueService detectionRisqueService;

    @InjectMocks
    private RisquePlanificateur planificateur;

    private Projet projetEnCours1;
    private Projet projetEnCours2;
    private Projet projetEnCours3;

    @BeforeEach
    void setUp() {
        projetEnCours1 = projet(1L, StatutProjet.EN_COURS);
        projetEnCours2 = projet(2L, StatutProjet.EN_COURS);
        projetEnCours3 = projet(3L, StatutProjet.EN_COURS);
    }

    @Test
    void detecter_neTraiteQueLesProjetsEnCoursSansDemandeur() {
        when(projetRepository.findAll()).thenReturn(Arrays.asList(
                projetEnCours1, projet(4L, StatutProjet.EN_PREPARATION), projet(5L, StatutProjet.CLOTURE),
                projet(6L, StatutProjet.SUSPENDU), projet(7L, StatutProjet.ARCHIVE), projetEnCours2));

        planificateur.detecterLesRisquesDesProjetsEnCours();

        verify(detectionRisqueService).detecter(1L, null);
        verify(detectionRisqueService).detecter(2L, null);
        verify(detectionRisqueService, never()).detecter(4L, null);
        verify(detectionRisqueService, never()).detecter(5L, null);
        verify(detectionRisqueService, never()).detecter(6L, null);
        verify(detectionRisqueService, never()).detecter(7L, null);
    }

    @Test
    void detecter_echecSurUnProjet_continueAvecLesSuivants() {
        when(projetRepository.findAll()).thenReturn(Arrays.asList(projetEnCours1, projetEnCours2, projetEnCours3));
        doThrow(new ReponseIaInvalideException("inexploitable")).when(detectionRisqueService).detecter(1L, null);
        doThrow(new AnalyseDejaEnCoursException(2L)).when(detectionRisqueService).detecter(2L, null);

        planificateur.detecterLesRisquesDesProjetsEnCours();

        verify(detectionRisqueService).detecter(3L, null);
    }

    @Test
    void detecter_assistantIndisponible_interrompLaTourneeSansAttendreLesAutresProjets() {
        when(projetRepository.findAll()).thenReturn(Arrays.asList(projetEnCours1, projetEnCours2, projetEnCours3));
        doThrow(new OllamaIndisponibleException("indisponible")).when(detectionRisqueService).detecter(1L, null);

        planificateur.detecterLesRisquesDesProjetsEnCours();

        verify(detectionRisqueService, never()).detecter(2L, null);
        verify(detectionRisqueService, never()).detecter(3L, null);
    }

    private Projet projet(Long id, StatutProjet statut) {
        Projet projet = new Projet();
        projet.setId(id);
        projet.setStatut(statut);
        return projet;
    }
}
