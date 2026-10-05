package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.client.KeycloakAdminClient;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurCreationRequest;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurModificationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurResponse;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.CompteSuspenduException;
import mg.gov.mef.dsi.gpdi.exception.KeycloakIndisponibleException;
import mg.gov.mef.dsi.gpdi.exception.RoleIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurAssigneATachesException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.UtilisateurMapper;
import mg.gov.mef.dsi.gpdi.repository.AffectationTacheRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.impl.UtilisateurServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UtilisateurMapper utilisateurMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UtilisateurCourantService utilisateurCourantService;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private AffectationTacheRepository affectationTacheRepository;

    private UtilisateurServiceImpl service;

    @BeforeEach
    void initService() {
        service = new UtilisateurServiceImpl(utilisateurRepository, roleRepository, utilisateurMapper, passwordEncoder, utilisateurCourantService, keycloakAdminClient, affectationTacheRepository);
    }

    @Test
    void creer_requeteValide_hacheLeMotDePasseEtEnregistre() {
        UtilisateurCreationRequest request = new UtilisateurCreationRequest();
        request.setMatricule("10001");
        request.setMotDePasse("motdepasse123");
        request.setIdRole(1L);

        Role role = new Role();
        role.setId(1L);

        Utilisateur utilisateurAEnregistrer = new Utilisateur();
        Utilisateur utilisateurEnregistre = new Utilisateur();
        utilisateurEnregistre.setId(1L);
        UtilisateurResponse reponseAttendue = new UtilisateurResponse(
                1L, "10001", "Nom", "Prenom", "email@test.mg", null, null, false, null, 1L, "ADMIN");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(utilisateurMapper.toEntity(request)).thenReturn(utilisateurAEnregistrer);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("HASH");
        when(utilisateurRepository.save(utilisateurAEnregistrer)).thenReturn(utilisateurEnregistre);
        when(utilisateurMapper.toResponse(utilisateurEnregistre)).thenReturn(reponseAttendue);

        UtilisateurResponse resultat = service.creer(request);

        assertThat(resultat).isEqualTo(reponseAttendue);
        assertThat(utilisateurAEnregistrer.getMotDePasse()).isEqualTo("HASH");
        assertThat(utilisateurAEnregistrer.getRole()).isEqualTo(role);
        verify(keycloakAdminClient).creerUtilisateur(eq("10001"), eq("motdepasse123"), any(), any(), any());
    }

    @Test
    void creer_provisioningKeycloakEnEchec_relanceLException() {
        UtilisateurCreationRequest request = new UtilisateurCreationRequest();
        request.setMatricule("10001");
        request.setMotDePasse("motdepasse123");
        request.setIdRole(1L);

        Role role = new Role();
        role.setId(1L);

        Utilisateur utilisateurAEnregistrer = new Utilisateur();
        Utilisateur utilisateurEnregistre = new Utilisateur();
        utilisateurEnregistre.setId(1L);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(utilisateurMapper.toEntity(request)).thenReturn(utilisateurAEnregistrer);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("HASH");
        when(utilisateurRepository.save(utilisateurAEnregistrer)).thenReturn(utilisateurEnregistre);
        doThrow(new KeycloakIndisponibleException("indisponible"))
                .when(keycloakAdminClient).creerUtilisateur(eq("10001"), eq("motdepasse123"), any(), any(), any());

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(KeycloakIndisponibleException.class);
    }

    @Test
    void creer_roleInexistant_leveRoleIntrouvableException() {
        UtilisateurCreationRequest request = new UtilisateurCreationRequest();
        request.setIdRole(999L);

        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(RoleIntrouvableException.class);
    }

    @Test
    void obtenir_utilisateurExistant_retourneLaReponseMappee() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        UtilisateurResponse reponseAttendue = new UtilisateurResponse(
                1L, "10001", "Nom", "Prenom", "email@test.mg", null, null, false, null, 1L, "ADMIN");

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurMapper.toResponse(utilisateur)).thenReturn(reponseAttendue);

        UtilisateurResponse resultat = service.obtenir(1L);

        assertThat(resultat).isEqualTo(reponseAttendue);
    }

    @Test
    void obtenir_utilisateurInexistant_leveUtilisateurIntrouvableException() {
        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(999L))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }

    @Test
    void modifier_sansMotDePasse_neChangePasLeMotDePasseExistant() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setMotDePasse("ANCIEN_HASH");

        Role role = new Role();
        role.setId(2L);

        UtilisateurModificationRequest request = new UtilisateurModificationRequest();
        request.setIdRole(2L);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(utilisateurRepository.save(utilisateur)).thenReturn(utilisateur);
        when(utilisateurMapper.toResponse(utilisateur)).thenReturn(
                new UtilisateurResponse(1L, null, null, null, null, null, null, false, null, 2L, null));

        service.modifier(1L, request);

        assertThat(utilisateur.getMotDePasse()).isEqualTo("ANCIEN_HASH");
        assertThat(utilisateur.getRole()).isEqualTo(role);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void modifier_avecNouveauMotDePasse_hacheEtRemplaceLeMotDePasse() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setMotDePasse("ANCIEN_HASH");

        Role role = new Role();
        role.setId(2L);

        UtilisateurModificationRequest request = new UtilisateurModificationRequest();
        request.setIdRole(2L);
        request.setMotDePasse("nouveauMotDePasse");

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("nouveauMotDePasse")).thenReturn("NOUVEAU_HASH");
        when(utilisateurRepository.save(utilisateur)).thenReturn(utilisateur);
        when(utilisateurMapper.toResponse(utilisateur)).thenReturn(
                new UtilisateurResponse(1L, null, null, null, null, null, null, false, null, 2L, null));

        service.modifier(1L, request);

        assertThat(utilisateur.getMotDePasse()).isEqualTo("NOUVEAU_HASH");
    }

    @Test
    void modifier_utilisateurInexistant_leveUtilisateurIntrouvableException() {
        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(999L, new UtilisateurModificationRequest()))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }

    @Test
    void supprimer_utilisateurExistant_appelleDeleteByIdEtSupprimeLeCompteKeycloak() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setMatricule("10001");
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        service.supprimer(1L, false);

        verify(utilisateurRepository).deleteById(1L);
        verify(keycloakAdminClient).supprimerUtilisateur("10001");
        verify(affectationTacheRepository, never()).deleteByUtilisateurId(1L);
    }

    @Test
    void supprimer_utilisateurAssigneATachesSansDetacher_leveUtilisateurAssigneATachesExceptionSansSupprimer() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setMatricule("10001");
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(affectationTacheRepository.existsByUtilisateurId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.supprimer(1L, false))
                .isInstanceOf(UtilisateurAssigneATachesException.class);

        verify(utilisateurRepository, never()).deleteById(1L);
        verify(keycloakAdminClient, never()).supprimerUtilisateur(anyString());
    }

    @Test
    void supprimer_utilisateurAssigneATachesAvecDetacher_retireLesAffectationsPuisSupprime() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setMatricule("10001");
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(affectationTacheRepository.existsByUtilisateurId(1L)).thenReturn(true);

        service.supprimer(1L, true);

        verify(affectationTacheRepository).deleteByUtilisateurId(1L);
        verify(utilisateurRepository).deleteById(1L);
        verify(keycloakAdminClient).supprimerUtilisateur("10001");
    }

    @Test
    void supprimer_utilisateurInexistant_leveUtilisateurIntrouvableException() {
        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimer(999L, false))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }

    @Test
    void obtenirCourant_delegueAuServiceDeContexteEtMappeLaReponse() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        UtilisateurResponse reponseAttendue = new UtilisateurResponse(
                1L, "10001", "Nom", "Prenom", "email@test.mg", null, null, true, null, 1L, "chef_projet");

        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(utilisateur);
        when(utilisateurMapper.toResponse(utilisateur)).thenReturn(reponseAttendue);

        UtilisateurResponse resultat = service.obtenirCourant();

        assertThat(resultat).isEqualTo(reponseAttendue);
    }

    @Test
    void obtenirCourant_utilisateurSuspendu_leveCompteSuspenduException() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setActif(false);

        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(utilisateur);

        assertThatThrownBy(() -> service.obtenirCourant())
                .isInstanceOf(CompteSuspenduException.class);
    }

    @Test
    void activer_utilisateurExistant_passeActifATrueEtEnregistre() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setActif(false);
        UtilisateurResponse reponseAttendue = new UtilisateurResponse(
                1L, null, null, null, null, null, null, true, null, null, null);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(utilisateur)).thenReturn(utilisateur);
        when(utilisateurMapper.toResponse(utilisateur)).thenReturn(reponseAttendue);

        UtilisateurResponse resultat = service.activer(1L);

        assertThat(utilisateur.getActif()).isTrue();
        assertThat(resultat).isEqualTo(reponseAttendue);
    }

    @Test
    void activer_utilisateurInexistant_leveUtilisateurIntrouvableException() {
        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activer(999L))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }

    @Test
    void suspendre_utilisateurExistant_passeActifAFauxEtEnregistre() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setActif(true);
        UtilisateurResponse reponseAttendue = new UtilisateurResponse(
                1L, null, null, null, null, null, null, false, null, null, null);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(utilisateur)).thenReturn(utilisateur);
        when(utilisateurMapper.toResponse(utilisateur)).thenReturn(reponseAttendue);

        UtilisateurResponse resultat = service.suspendre(1L);

        assertThat(utilisateur.getActif()).isFalse();
        assertThat(resultat).isEqualTo(reponseAttendue);
    }

    @Test
    void suspendre_utilisateurInexistant_leveUtilisateurIntrouvableException() {
        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspendre(999L))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }
}
