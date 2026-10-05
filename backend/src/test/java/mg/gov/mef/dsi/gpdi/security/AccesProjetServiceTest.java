package mg.gov.mef.dsi.gpdi.security;

import mg.gov.mef.dsi.gpdi.entity.Equipe;
import mg.gov.mef.dsi.gpdi.entity.Projet;
import mg.gov.mef.dsi.gpdi.entity.Role;
import mg.gov.mef.dsi.gpdi.entity.StatutPartage;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException;
import mg.gov.mef.dsi.gpdi.repository.MembreEquipeRepository;
import mg.gov.mef.dsi.gpdi.repository.PartageClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesProjetServiceTest {

    private static final Long ID_PROJET = 1L;
    private static final Long ID_EQUIPE = 3L;

    @Mock
    private UtilisateurCourantService utilisateurCourantService;
    @Mock
    private MembreEquipeRepository membreEquipeRepository;
    @Mock
    private PartageClientRepository partageClientRepository;

    private AccesProjetService service;
    private Projet projet;
    private Utilisateur responsable;
    private Utilisateur chefEquipe;
    private Utilisateur membre;
    private Utilisateur inconnu;
    private Utilisateur directeurDsi;
    private Utilisateur clientAvecPartage;

    @BeforeEach
    void setUp() {
        service = new AccesProjetService(utilisateurCourantService, membreEquipeRepository, partageClientRepository);

        responsable = utilisateur(10L, null);
        chefEquipe = utilisateur(11L, null);
        membre = utilisateur(12L, null);
        inconnu = utilisateur(13L, null);
        directeurDsi = utilisateur(14L, "directeur_dsi");
        clientAvecPartage = utilisateur(15L, "direction_metier");

        Equipe equipe = new Equipe();
        equipe.setId(ID_EQUIPE);
        equipe.setChefEquipe(chefEquipe);

        projet = new Projet();
        projet.setId(ID_PROJET);
        projet.setResponsable(responsable);
        projet.setEquipe(equipe);

        lenient().when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(ID_EQUIPE, membre.getId())).thenReturn(true);
        lenient().when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(ID_EQUIPE, inconnu.getId())).thenReturn(false);
        lenient().when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(ID_EQUIPE, directeurDsi.getId())).thenReturn(false);
        lenient().when(membreEquipeRepository.existsByEquipeIdAndUtilisateurId(ID_EQUIPE, clientAvecPartage.getId())).thenReturn(false);
    }

    private Utilisateur utilisateur(Long id, String nomRole) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        if (nomRole != null) {
            Role role = new Role();
            role.setNom(nomRole);
            utilisateur.setRole(role);
        }
        return utilisateur;
    }

    @Test
    void verifierMembreDuProjet_responsable_neLevePasDException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(responsable);

        assertThatCode(() -> service.verifierMembreDuProjet(projet)).doesNotThrowAnyException();
    }

    @Test
    void verifierMembreDuProjet_chefDEquipe_neLevePasDException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(chefEquipe);

        assertThatCode(() -> service.verifierMembreDuProjet(projet)).doesNotThrowAnyException();
    }

    @Test
    void verifierMembreDuProjet_membreDeLEquipe_neLevePasDException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(membre);

        assertThatCode(() -> service.verifierMembreDuProjet(projet)).doesNotThrowAnyException();
    }

    @Test
    void verifierMembreDuProjet_utilisateurEtranger_leveAccesProjetRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(inconnu);

        assertThatThrownBy(() -> service.verifierMembreDuProjet(projet)).isInstanceOf(AccesProjetRefuseException.class);
    }

    @Test
    void verifierMembreDuProjet_directeurDsiNonMembre_leveAccesProjetRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(directeurDsi);

        assertThatThrownBy(() -> service.verifierMembreDuProjet(projet)).isInstanceOf(AccesProjetRefuseException.class);
    }

    @Test
    void verifierMembreDuProjet_projetSansEquipe_seulLeResponsableEstAccepte() {
        Projet projetSansEquipe = new Projet();
        projetSansEquipe.setId(2L);
        projetSansEquipe.setResponsable(responsable);
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(membre);

        assertThatThrownBy(() -> service.verifierMembreDuProjet(projetSansEquipe)).isInstanceOf(AccesProjetRefuseException.class);
    }

    @Test
    void verifierAccesLecture_membreDeLEquipe_neLevePasDException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(membre);

        assertThatCode(() -> service.verifierAccesLecture(projet)).doesNotThrowAnyException();
    }

    @Test
    void verifierAccesLecture_directeurDsiNonMembre_neLevePasDException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(directeurDsi);

        assertThatCode(() -> service.verifierAccesLecture(projet)).doesNotThrowAnyException();
    }

    @Test
    void verifierAccesLecture_utilisateurEtranger_leveAccesProjetRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(inconnu);

        assertThatThrownBy(() -> service.verifierAccesLecture(projet)).isInstanceOf(AccesProjetRefuseException.class);
    }

    @Test
    void verifierAccesLecturePartageable_clientAvecPartageActif_neLevePasDException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(clientAvecPartage);
        when(partageClientRepository.existsByClientIdAndProjetIdAndStatut(clientAvecPartage.getId(), ID_PROJET, StatutPartage.ACTIF))
                .thenReturn(true);

        assertThatCode(() -> service.verifierAccesLecturePartageable(projet)).doesNotThrowAnyException();
    }

    @Test
    void verifierAccesLecturePartageable_clientSansPartageActif_leveAccesProjetRefuseException() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(clientAvecPartage);
        when(partageClientRepository.existsByClientIdAndProjetIdAndStatut(clientAvecPartage.getId(), ID_PROJET, StatutPartage.ACTIF))
                .thenReturn(false);

        assertThatThrownBy(() -> service.verifierAccesLecturePartageable(projet)).isInstanceOf(AccesProjetRefuseException.class);
    }

    @Test
    void verifierAccesLecturePartageable_directeurDsi_neLevePasDExceptionSansVerifierLePartage() {
        when(utilisateurCourantService.obtenirUtilisateurCourant()).thenReturn(directeurDsi);

        assertThatCode(() -> service.verifierAccesLecturePartageable(projet)).doesNotThrowAnyException();
    }
}
