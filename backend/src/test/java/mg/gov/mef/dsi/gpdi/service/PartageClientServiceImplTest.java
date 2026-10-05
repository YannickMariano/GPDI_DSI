package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.PartageClientRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PartageClientResponse;
import mg.gov.mef.dsi.gpdi.entity.PartageClient;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.PartageClientIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.PartageDejaActifException;
import mg.gov.mef.dsi.gpdi.exception.StatutPartageInvalideException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonEligibleException;
import mg.gov.mef.dsi.gpdi.mapper.PartageClientMapper;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.impl.PartageClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartageClientServiceImplTest {

    @Mock
    private PartageClientRepository partageClientRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ProjetRepository projetRepository;
    @Mock
    private PartageClientMapper partageClientMapper;
    @Mock
    private HabilitationService habilitationService;

    private PartageClientServiceImpl service;

    @BeforeEach
    void initService() {
        service = new PartageClientServiceImpl(partageClientRepository, utilisateurRepository, projetRepository, partageClientMapper, habilitationService);
    }

    private Utilisateur clientEligible(Long id) {
        Role role = new Role();
        role.setNom("direction_metier");
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setRole(role);
        return utilisateur;
    }

    @Test
    void creer_projetsSansPartageActif_creeUnePartageParProjet() {
        Utilisateur client = clientEligible(1L);
        Projet projetA = new Projet();
        projetA.setId(10L);
        Projet projetB = new Projet();
        projetB.setId(20L);

        PartageClientRequest request = new PartageClientRequest();
        request.setIdUtilisateur(1L);
        request.setIdsProjets(Arrays.asList(10L, 20L));
        request.setDateEcheance(LocalDate.now().plusMonths(1));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(client));
        when(projetRepository.findById(10L)).thenReturn(Optional.of(projetA));
        when(projetRepository.findById(20L)).thenReturn(Optional.of(projetB));
        when(partageClientRepository.existsByClientIdAndProjetIdAndStatut(1L, 10L, StatutPartage.ACTIF)).thenReturn(false);
        when(partageClientRepository.existsByClientIdAndProjetIdAndStatut(1L, 20L, StatutPartage.ACTIF)).thenReturn(false);
        when(partageClientRepository.save(any(PartageClient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partageClientMapper.toResponse(any(PartageClient.class))).thenReturn(new PartageClientResponse());

        List<PartageClientResponse> reponses = service.creer(request);

        assertThat(reponses).hasSize(2);
    }

    @Test
    void creer_unProjetDejaPartageActivement_leveExceptionSansRienCreer() {
        Utilisateur client = clientEligible(1L);
        Projet projetA = new Projet();
        projetA.setId(10L);

        PartageClientRequest request = new PartageClientRequest();
        request.setIdUtilisateur(1L);
        request.setIdsProjets(Collections.singletonList(10L));
        request.setDateEcheance(LocalDate.now().plusMonths(1));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(client));
        when(projetRepository.findById(10L)).thenReturn(Optional.of(projetA));
        when(partageClientRepository.existsByClientIdAndProjetIdAndStatut(1L, 10L, StatutPartage.ACTIF)).thenReturn(true);

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(PartageDejaActifException.class);
    }

    @Test
    void creer_utilisateurSansRoleClient_leveUtilisateurNonEligibleException() {
        Role role = new Role();
        role.setNom("DEVELOPPEUR");
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setRole(role);

        PartageClientRequest request = new PartageClientRequest();
        request.setIdUtilisateur(1L);
        request.setIdsProjets(Collections.singletonList(10L));
        request.setDateEcheance(LocalDate.now().plusMonths(1));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(UtilisateurNonEligibleException.class);
    }

    @Test
    void revoquer_partageActif_passeEnRevoque() {
        PartageClient partage = new PartageClient();
        partage.setId(1L);
        partage.setStatut(StatutPartage.ACTIF);

        when(partageClientRepository.findById(1L)).thenReturn(Optional.of(partage));
        when(partageClientRepository.save(partage)).thenReturn(partage);
        when(partageClientMapper.toResponse(partage)).thenReturn(new PartageClientResponse());

        service.revoquer(1L);

        assertThat(partage.getStatut()).isEqualTo(StatutPartage.REVOQUE);
    }

    @Test
    void revoquer_partageDejaRevoque_leveStatutPartageInvalideException() {
        PartageClient partage = new PartageClient();
        partage.setId(1L);
        partage.setStatut(StatutPartage.REVOQUE);

        when(partageClientRepository.findById(1L)).thenReturn(Optional.of(partage));

        assertThatThrownBy(() -> service.revoquer(1L))
                .isInstanceOf(StatutPartageInvalideException.class);
    }

    @Test
    void revoquer_partageInexistant_levePartageClientIntrouvableException() {
        when(partageClientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoquer(999L))
                .isInstanceOf(PartageClientIntrouvableException.class);
    }

    @Test
    void expirerLesPartagesEnRetard_partageActifEcheanceDepassee_passeEnExpire() {
        PartageClient partage = new PartageClient();
        partage.setId(1L);
        partage.setStatut(StatutPartage.ACTIF);

        when(partageClientRepository.findByStatutAndDateEcheanceLessThan(StatutPartage.ACTIF, LocalDate.now()))
                .thenReturn(Collections.singletonList(partage));
        when(partageClientRepository.save(partage)).thenReturn(partage);

        service.expirerLesPartagesEnRetard();

        assertThat(partage.getStatut()).isEqualTo(StatutPartage.EXPIRE);
    }
}
