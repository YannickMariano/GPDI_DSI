package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.EquipeRequest;
import mg.gov.mef.dsi.gpdi.dto.response.EquipeResponse;
import mg.gov.mef.dsi.gpdi.dto.response.MembreEquipeResponse;
import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.MembreEquipe;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.EquipeIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.EquipeRattacheeProjetException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurNonEligibleException;
import mg.gov.mef.dsi.gpdi.mapper.EquipeMapper;
import mg.gov.mef.dsi.gpdi.repository.EquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.ProjetRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.impl.EquipeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipeServiceImplTest {

    @Mock
    private EquipeRepository equipeRepository;

    @Mock
    private MembreEquipeRepository membreEquipeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private EquipeMapper equipeMapper;

    private EquipeServiceImpl service;

    @BeforeEach
    void initService() {
        service = new EquipeServiceImpl(equipeRepository, membreEquipeRepository, utilisateurRepository, projetRepository, equipeMapper);
    }

    private Utilisateur utilisateurAvecRole(Long id, String nomRole) {
        Role role = new Role();
        role.setNom(nomRole);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setRole(role);
        return utilisateur;
    }

    @Test
    void creer_chefEtMembresEligibles_creeLequipeEtSesMembres() {
        EquipeRequest request = new EquipeRequest();
        request.setNom("Equipe A");
        request.setIdChefEquipe(1L);
        request.setIdsMembres(Collections.singletonList(2L));

        Utilisateur chef = utilisateurAvecRole(1L, "membre_equipe");
        Utilisateur membre = utilisateurAvecRole(2L, "membre_equipe");

        Equipe equipeAEnregistrer = new Equipe();
        Equipe equipeEnregistree = new Equipe();
        equipeEnregistree.setId(10L);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(membre));
        when(equipeMapper.toEntity(request)).thenReturn(equipeAEnregistrer);
        when(equipeRepository.save(equipeAEnregistrer)).thenReturn(equipeEnregistree);

        MembreEquipe membreEnregistre = new MembreEquipe();
        when(membreEquipeRepository.save(any(MembreEquipe.class))).thenReturn(membreEnregistre);
        when(equipeMapper.toMembreResponse(membreEnregistre))
                .thenReturn(new MembreEquipeResponse(1L, 2L, "M002", "Nom", "Prenom", true));

        EquipeResponse reponseAttendue = new EquipeResponse();
        reponseAttendue.setId(10L);
        when(equipeMapper.toResponse(equipeEnregistree)).thenReturn(reponseAttendue);

        EquipeResponse resultat = service.creer(request);

        assertThat(resultat.getId()).isEqualTo(10L);
        assertThat(resultat.getMembres()).hasSize(1);
        assertThat(equipeAEnregistrer.getChefEquipe()).isEqualTo(chef);
    }

    @Test
    void creer_chefProjetCommeChefEtMembre_creeLequipe() {
        EquipeRequest request = new EquipeRequest();
        request.setNom("Equipe B");
        request.setIdChefEquipe(1L);
        request.setIdsMembres(Collections.singletonList(2L));

        Utilisateur chef = utilisateurAvecRole(1L, "chef_projet");
        Utilisateur membre = utilisateurAvecRole(2L, "chef_projet");

        Equipe equipeAEnregistrer = new Equipe();
        Equipe equipeEnregistree = new Equipe();
        equipeEnregistree.setId(11L);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(membre));
        when(equipeMapper.toEntity(request)).thenReturn(equipeAEnregistrer);
        when(equipeRepository.save(equipeAEnregistrer)).thenReturn(equipeEnregistree);

        MembreEquipe membreEnregistre = new MembreEquipe();
        when(membreEquipeRepository.save(any(MembreEquipe.class))).thenReturn(membreEnregistre);
        when(equipeMapper.toMembreResponse(membreEnregistre))
                .thenReturn(new MembreEquipeResponse(1L, 2L, "M002", "Nom", "Prenom", true));

        EquipeResponse reponseAttendue = new EquipeResponse();
        reponseAttendue.setId(11L);
        when(equipeMapper.toResponse(equipeEnregistree)).thenReturn(reponseAttendue);

        EquipeResponse resultat = service.creer(request);

        assertThat(resultat.getId()).isEqualTo(11L);
        assertThat(equipeAEnregistrer.getChefEquipe()).isEqualTo(chef);
    }

    @Test
    void creer_chefNonEligible_leveUtilisateurNonEligibleException() {
        EquipeRequest request = new EquipeRequest();
        request.setIdChefEquipe(1L);
        request.setIdsMembres(Collections.singletonList(2L));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateurAvecRole(1L, "administrateur")));

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(UtilisateurNonEligibleException.class);
    }

    @Test
    void creer_chefInexistant_leveUtilisateurIntrouvableException() {
        EquipeRequest request = new EquipeRequest();
        request.setIdChefEquipe(999L);
        request.setIdsMembres(Collections.singletonList(2L));

        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }

    @Test
    void obtenir_equipeInexistante_leveEquipeIntrouvableException() {
        when(equipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(999L))
                .isInstanceOf(EquipeIntrouvableException.class);
    }

    @Test
    void modifier_retireUnMembreAbsentDeLaNouvelleListe_supprimeLaLigne() {
        Equipe equipe = new Equipe();
        equipe.setId(10L);

        EquipeRequest request = new EquipeRequest();
        request.setIdChefEquipe(1L);
        request.setIdsMembres(Collections.singletonList(3L));

        Utilisateur chef = utilisateurAvecRole(1L, "membre_equipe");
        Utilisateur nouveauMembre = utilisateurAvecRole(3L, "membre_equipe");

        MembreEquipe membreARetirer = new MembreEquipe();
        Utilisateur ancienMembre = new Utilisateur();
        ancienMembre.setId(2L);
        membreARetirer.setUtilisateur(ancienMembre);

        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(utilisateurRepository.findById(3L)).thenReturn(Optional.of(nouveauMembre));
        when(membreEquipeRepository.findByEquipeId(10L))
                .thenReturn(Collections.singletonList(membreARetirer))
                .thenReturn(Collections.emptyList());
        lenient().when(equipeRepository.save(equipe)).thenReturn(equipe);
        lenient().when(membreEquipeRepository.save(any(MembreEquipe.class))).thenReturn(new MembreEquipe());
        when(equipeMapper.toResponse(equipe)).thenReturn(new EquipeResponse());

        service.modifier(10L, request);

        verify(membreEquipeRepository).deleteAll(Collections.singletonList(membreARetirer));
    }

    @Test
    void modifier_equipeInexistante_leveEquipeIntrouvableException() {
        when(equipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(999L, new EquipeRequest()))
                .isInstanceOf(EquipeIntrouvableException.class);
    }

    @Test
    void supprimer_equipeExistante_supprimeLesMembresPuisLequipe() {
        when(equipeRepository.existsById(10L)).thenReturn(true);

        service.supprimer(10L);

        verify(membreEquipeRepository).deleteByEquipeId(10L);
        verify(equipeRepository).deleteById(10L);
    }

    @Test
    void supprimer_equipeRattacheeAUnProjet_leveEquipeRattacheeProjetExceptionEtNeSupprimeRien() {
        when(equipeRepository.existsById(10L)).thenReturn(true);
        when(projetRepository.existsByEquipeId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.supprimer(10L))
                .isInstanceOf(EquipeRattacheeProjetException.class);

        verify(membreEquipeRepository, never()).deleteByEquipeId(10L);
        verify(equipeRepository, never()).deleteById(10L);
    }

    @Test
    void supprimer_equipeInexistante_leveEquipeIntrouvableException() {
        when(equipeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.supprimer(999L))
                .isInstanceOf(EquipeIntrouvableException.class);
    }
}
