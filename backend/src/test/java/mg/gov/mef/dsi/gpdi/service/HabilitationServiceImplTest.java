package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.HabilitationLigneRequest;
import mg.gov.mef.dsi.gpdi.dto.response.HabilitationResponse;
import mg.gov.mef.dsi.gpdi.entity.Habilitation;
import mg.gov.mef.dsi.gpdi.entity.NiveauAcces;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.HabilitationInvalideException;
import mg.gov.mef.dsi.gpdi.exception.HabilitationRefuseeException;
import mg.gov.mef.dsi.gpdi.repository.HabilitationRepository;
import mg.gov.mef.dsi.gpdi.repository.RoleRepository;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.impl.HabilitationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabilitationServiceImplTest {

    @Mock
    private HabilitationRepository habilitationRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourantService;

    private HabilitationServiceImpl service;

    @BeforeEach
    void initService() {
        service = new HabilitationServiceImpl(habilitationRepository, roleRepository, utilisateurCourantService);
    }

    private Habilitation habilitation(String fonction, String roleNom, NiveauAcces niveau) {
        Habilitation habilitation = new Habilitation();
        habilitation.setModule("Module 2");
        habilitation.setFonction(fonction);
        habilitation.setRoleNom(roleNom);
        habilitation.setNiveau(niveau);
        return habilitation;
    }

    private Utilisateur utilisateurAvecRole(String roleNom) {
        Utilisateur utilisateur = new Utilisateur();
        if (roleNom != null) {
            Role role = new Role();
            role.setNom(roleNom);
            utilisateur.setRole(role);
        }
        return utilisateur;
    }

    @Test
    void rechercherMatrice_plusieursRolesPourUneFonction_regroupeParFonction() {
        when(habilitationRepository.findAllByOrderByIdAsc()).thenReturn(Arrays.asList(
                habilitation("Création des tâches", "chef_projet", NiveauAcces.ECRITURE),
                habilitation("Création des tâches", "membre_equipe", NiveauAcces.LECTURE)));

        List<HabilitationResponse> resultat = service.rechercherMatrice();

        assertThat(resultat).hasSize(1);
        HabilitationResponse ligne = resultat.get(0);
        assertThat(ligne.getFonction()).isEqualTo("Création des tâches");
        assertThat(ligne.getModule()).isEqualTo("Module 2");
        assertThat(ligne.getAcces()).containsEntry("chef_projet", "ecriture").containsEntry("membre_equipe", "lecture");
    }

    @Test
    void mettreAJourMatrice_ligneValide_metAJourLeNiveau() {
        Habilitation existante = habilitation("Création des tâches", "chef_projet", NiveauAcces.LECTURE);
        when(roleRepository.existsByNom("chef_projet")).thenReturn(true);
        when(habilitationRepository.findByFonctionAndRoleNom("Création des tâches", "chef_projet"))
                .thenReturn(Optional.of(existante));

        HabilitationLigneRequest ligne = new HabilitationLigneRequest();
        ligne.setFonction("Création des tâches");
        ligne.setRoleNom("chef_projet");
        ligne.setNiveau("ecriture");

        service.mettreAJourMatrice(Collections.singletonList(ligne));

        assertThat(existante.getNiveau()).isEqualTo(NiveauAcces.ECRITURE);
    }

    @Test
    void mettreAJourMatrice_roleInconnu_leveHabilitationInvalideException() {
        when(roleRepository.existsByNom("role_fantome")).thenReturn(false);

        HabilitationLigneRequest ligne = new HabilitationLigneRequest();
        ligne.setFonction("Création des tâches");
        ligne.setRoleNom("role_fantome");
        ligne.setNiveau("ecriture");

        assertThatThrownBy(() -> service.mettreAJourMatrice(Collections.singletonList(ligne)))
                .isInstanceOf(HabilitationInvalideException.class);
    }

    @Test
    void mettreAJourMatrice_fonctionInconnue_leveHabilitationInvalideException() {
        when(roleRepository.existsByNom("chef_projet")).thenReturn(true);
        when(habilitationRepository.findByFonctionAndRoleNom("Fonction fantome", "chef_projet"))
                .thenReturn(Optional.empty());

        HabilitationLigneRequest ligne = new HabilitationLigneRequest();
        ligne.setFonction("Fonction fantome");
        ligne.setRoleNom("chef_projet");
        ligne.setNiveau("ecriture");

        assertThatThrownBy(() -> service.mettreAJourMatrice(Collections.singletonList(ligne)))
                .isInstanceOf(HabilitationInvalideException.class);
    }

    @Test
    void mettreAJourMatrice_niveauInvalide_leveHabilitationInvalideException() {
        when(roleRepository.existsByNom("chef_projet")).thenReturn(true);

        HabilitationLigneRequest ligne = new HabilitationLigneRequest();
        ligne.setFonction("Création des tâches");
        ligne.setRoleNom("chef_projet");
        ligne.setNiveau("super_pouvoir");

        assertThatThrownBy(() -> service.mettreAJourMatrice(Collections.singletonList(ligne)))
                .isInstanceOf(HabilitationInvalideException.class);
    }

    @Test
    void verifierEcriture_roleAvecEcriture_neLevePasException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(utilisateurAvecRole("chef_projet"));
        when(habilitationRepository.findByFonctionAndRoleNom("Création des tâches", "chef_projet"))
                .thenReturn(Optional.of(habilitation("Création des tâches", "chef_projet", NiveauAcces.ECRITURE)));

        service.verifierEcriture("Création des tâches");
    }

    @Test
    void verifierEcriture_roleAvecLectureSeule_leveHabilitationRefuseeException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(utilisateurAvecRole("membre_equipe"));
        when(habilitationRepository.findByFonctionAndRoleNom("Création des tâches", "membre_equipe"))
                .thenReturn(Optional.of(habilitation("Création des tâches", "membre_equipe", NiveauAcces.LECTURE)));

        assertThatThrownBy(() -> service.verifierEcriture("Création des tâches"))
                .isInstanceOf(HabilitationRefuseeException.class);
    }

    @Test
    void verifierEcriture_aucuneLigneCorrespondante_leveHabilitationRefuseeException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(utilisateurAvecRole("direction_metier"));
        when(habilitationRepository.findByFonctionAndRoleNom("Création des tâches", "direction_metier"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifierEcriture("Création des tâches"))
                .isInstanceOf(HabilitationRefuseeException.class);
    }

    @Test
    void verifierEcriture_utilisateurSansRole_leveHabilitationRefuseeException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(utilisateurAvecRole(null));

        assertThatThrownBy(() -> service.verifierEcriture("Création des tâches"))
                .isInstanceOf(HabilitationRefuseeException.class);
    }
}
