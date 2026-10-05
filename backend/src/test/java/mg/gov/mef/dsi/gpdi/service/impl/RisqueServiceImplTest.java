package mg.gov.mef.dsi.gpdi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;
import mg.gov.mef.dsi.gpdi.entity.IaRecommandation;
import mg.gov.mef.dsi.gpdi.entity.NiveauRisque;
import mg.gov.mef.dsi.gpdi.entity.Risque;
import mg.gov.mef.dsi.gpdi.entity.StatutRecommandation;
import mg.gov.mef.dsi.gpdi.entity.StatutRisque;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.ProjetIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.IaRecommandationMapper;
import mg.gov.mef.dsi.gpdi.mapper.RisqueMapper;
import mg.gov.mef.dsi.gpdi.repository.IaRecommandationRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.RisqueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RisqueServiceImplTest {

    private static final Long ID_PROJET = 1L;

    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private RisqueRepository risqueRepository;
    @Mock
    private IaRecommandationRepository iaRecommandationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RisqueServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RisqueServiceImpl(projetRepository, risqueRepository, iaRecommandationRepository,
                Mappers.getMapper(RisqueMapper.class), Mappers.getMapper(IaRecommandationMapper.class));
    }

    @Test
    void rechercher_projetInconnu_leveProjetIntrouvableException() {
        when(projetRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.rechercher(99L)).isInstanceOf(ProjetIntrouvableException.class);
    }

    @Test
    void rechercher_projetSansRisque_retourneUneListeVideSansRequeteSurLesSolutions() {
        when(projetRepository.existsById(ID_PROJET)).thenReturn(true);
        when(risqueRepository.findByProjetIdOrderByIdAsc(ID_PROJET)).thenReturn(Collections.<Risque>emptyList());

        assertThat(service.rechercher(ID_PROJET)).isEmpty();

        verify(iaRecommandationRepository, never()).findByRisqueIdInOrderByIdAsc(anyList());
    }

    @Test
    void rechercher_risques_lesNumerotePropageLesChampsEtRattacheLHistoriqueCompletDesSolutions() {
        Utilisateur porteur = new Utilisateur();
        porteur.setId(7L);
        porteur.setPrenom("Lala");
        porteur.setNom("Rabe");
        LocalDateTime detection = LocalDateTime.of(2026, 9, 21, 2, 0);
        Risque premier = risque(10L, "Tâches bloquées", StatutRisque.OUVERT, porteur);
        premier.setDateCreation(detection);
        Risque second = risque(25L, "Surcharge", StatutRisque.SOUS_CONTROLE, null);
        when(projetRepository.existsById(ID_PROJET)).thenReturn(true);
        when(risqueRepository.findByProjetIdOrderByIdAsc(ID_PROJET)).thenReturn(Arrays.asList(premier, second));
        // L'historique complet est renvoye, y compris les solutions deja traitees (affichees en lecture seule).
        when(iaRecommandationRepository.findByRisqueIdInOrderByIdAsc(Arrays.asList(10L, 25L)))
                .thenReturn(Arrays.asList(
                        solution(1L, premier, "Confier T-1", "2 jours", StatutRecommandation.PROPOSEE),
                        solution(2L, premier, "Revue", null, StatutRecommandation.REJETEE)));

        List<RisqueResponse> reponses = service.rechercher(ID_PROJET);

        assertThat(reponses).hasSize(2);
        RisqueResponse r1 = reponses.get(0);
        assertThat(r1.getId()).isEqualTo(10L);
        assertThat(r1.getCode()).isEqualTo("R1");
        assertThat(r1.getLibelle()).isEqualTo("Tâches bloquées");
        assertThat(r1.getParade()).isEqualTo("Parade de Tâches bloquées");
        assertThat(r1.getAnalyse()).isEqualTo("Analyse de Tâches bloquées");
        assertThat(r1.getProbabilite()).isEqualTo(4);
        assertThat(r1.getImpact()).isEqualTo(5);
        assertThat(r1.getNiveau()).isEqualTo(NiveauRisque.CRITIQUE);
        assertThat(r1.getStatut()).isEqualTo(StatutRisque.OUVERT);
        assertThat(r1.getDateDetection()).isEqualTo(detection);
        assertThat(r1.getIdPorteur()).isEqualTo(7L);
        assertThat(r1.getPorteur()).isEqualTo("Lala Rabe");
        assertThat(r1.getSolutions()).hasSize(2);
        assertThat(r1.getSolutions().get(0).getLibelle()).isEqualTo("Confier T-1");
        assertThat(r1.getSolutions().get(0).getGain()).isEqualTo("2 jours");
        assertThat(r1.getSolutions().get(0).getStatut()).isEqualTo("PROPOSEE");
        assertThat(r1.getSolutions().get(1).getGain()).isNull();
        assertThat(r1.getSolutions().get(1).getStatut()).isEqualTo("REJETEE");

        RisqueResponse r2 = reponses.get(1);
        assertThat(r2.getCode()).isEqualTo("R2");
        assertThat(r2.getIdPorteur()).isNull();
        assertThat(r2.getPorteur()).isNull();
        assertThat(r2.getSolutions()).isEmpty();
    }

    private Risque risque(Long id, String libelle, StatutRisque statut, Utilisateur porteur) {
        Risque risque = new Risque();
        risque.setId(id);
        risque.setLibelle(libelle);
        risque.setDescription("Analyse de " + libelle);
        risque.setMesureMitigation("Parade de " + libelle);
        risque.setProbabilite(4);
        risque.setImpact(5);
        risque.setNiveau(NiveauRisque.CRITIQUE);
        risque.setStatut(statut);
        risque.setPorteur(porteur);
        return risque;
    }

    private IaRecommandation solution(Long id, Risque risque, String libelle, String gain, StatutRecommandation statut) {
        IaRecommandation solution = new IaRecommandation();
        solution.setId(id);
        solution.setRisque(risque);
        solution.setLibelle(libelle);
        solution.setStatut(statut);
        solution.setActionProposee(gain == null
                ? objectMapper.createObjectNode().put("type", "CREER_TACHE")
                : objectMapper.createObjectNode().put("type", "CREER_TACHE").put("gain", gain));
        return solution;
    }
}
