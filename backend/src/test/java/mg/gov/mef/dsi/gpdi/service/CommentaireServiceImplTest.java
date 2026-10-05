package mg.gov.mef.dsi.gpdi.service;

import mg.gov.mef.dsi.gpdi.dto.request.CommentaireRequest;
import mg.gov.mef.dsi.gpdi.dto.response.CommentaireResponse;
import mg.gov.mef.dsi.gpdi.entity.Commentaire;
import mg.gov.mef.dsi.gpdi.entity.PieceJointeCommentaire;
import mg.gov.mef.dsi.gpdi.entity.Tache;
import mg.gov.mef.dsi.gpdi.entity.Utilisateur;
import mg.gov.mef.dsi.gpdi.exception.PieceJointeIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.PieceJointeInvalideException;
import mg.gov.mef.dsi.gpdi.exception.TacheIntrouvableException;
import mg.gov.mef.dsi.gpdi.exception.UtilisateurIntrouvableException;
import mg.gov.mef.dsi.gpdi.mapper.TacheMapper;
import mg.gov.mef.dsi.gpdi.repository.CommentaireRepository;
import mg.gov.mef.dsi.gpdi.repository.PieceJointeCommentaireRepository;
import mg.gov.mef.dsi.gpdi.repository.TacheRepository;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.security.AccesProjetService;
import mg.gov.mef.dsi.gpdi.service.impl.CommentaireServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentaireServiceImplTest {

    @Mock
    private CommentaireRepository commentaireRepository;
    @Mock
    private TacheRepository tacheRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private TacheMapper tacheMapper;
    @Mock
    private AccesProjetService accesProjetService;
    @Mock
    private PieceJointeCommentaireRepository pieceJointeCommentaireRepository;

    private CommentaireServiceImpl service;

    @BeforeEach
    void initService() {
        service = new CommentaireServiceImpl(commentaireRepository, tacheRepository, utilisateurRepository, tacheMapper, accesProjetService, pieceJointeCommentaireRepository);
    }

    @Test
    void creer_tacheEtAuteurExistants_enregistreLeCommentaire() {
        Tache tache = new Tache();
        tache.setId(1L);
        Utilisateur auteur = new Utilisateur();
        auteur.setId(5L);

        CommentaireRequest request = new CommentaireRequest();
        request.setContenu("Bon travail");

        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(auteur));
        when(commentaireRepository.save(any(Commentaire.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tacheMapper.toCommentaireResponse(any(Commentaire.class))).thenReturn(new CommentaireResponse(1L, 5L, "Prenom Nom", "Bon travail", null, null));

        service.creer(1L, 5L, request);

        assertThat(request.getContenu()).isEqualTo("Bon travail");
    }

    @Test
    void creer_tacheInexistante_leveTacheIntrouvableException() {
        when(tacheRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(999L, 5L, new CommentaireRequest()))
                .isInstanceOf(TacheIntrouvableException.class);
    }

    @Test
    void creer_auteurInexistant_leveUtilisateurIntrouvableException() {
        Tache tache = new Tache();
        tache.setId(1L);
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creer(1L, 999L, new CommentaireRequest()))
                .isInstanceOf(UtilisateurIntrouvableException.class);
    }

    @Test
    void creer_utilisateurNonMembreDuProjet_neCreePasLeCommentaire() {
        Tache tache = new Tache();
        tache.setId(1L);
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        doThrow(new mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException(1L))
                .when(accesProjetService).verifierMembreDuProjet(tache.getProjet());

        assertThatThrownBy(() -> service.creer(1L, 5L, new CommentaireRequest()))
                .isInstanceOf(mg.gov.mef.dsi.gpdi.exception.AccesProjetRefuseException.class);

        verify(commentaireRepository, never()).save(any());
    }

    @Test
    void rechercher_tacheInexistante_leveTacheIntrouvableException() {
        when(tacheRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rechercher(999L))
                .isInstanceOf(TacheIntrouvableException.class);
    }

    @Test
    void rechercher_tacheExistante_retourneLesCommentairesMappes() {
        Tache tache = new Tache();
        tache.setId(1L);
        Commentaire commentaire = new Commentaire();
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(commentaireRepository.findByTacheIdOrderByDateCreationAsc(1L))
                .thenReturn(Collections.singletonList(commentaire));
        when(tacheMapper.toCommentaireResponse(commentaire)).thenReturn(new CommentaireResponse(1L, 5L, "Prenom Nom", "Bon travail", null, null));

        assertThat(service.rechercher(1L)).hasSize(1);
        verify(accesProjetService).verifierAccesLecture(tache.getProjet());
    }

    @Test
    void creerAvecPieceJointe_fichierValide_enregistreLeCommentaireEtLaPieceJointe() {
        Tache tache = new Tache();
        tache.setId(1L);
        Utilisateur auteur = new Utilisateur();
        auteur.setId(5L);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cahier-des-charges.pdf", "application/pdf", "contenu".getBytes());

        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(auteur));
        when(commentaireRepository.save(any(Commentaire.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pieceJointeCommentaireRepository.save(any(PieceJointeCommentaire.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tacheMapper.toCommentaireResponse(any(Commentaire.class)))
                .thenReturn(new CommentaireResponse(1L, 5L, "Prenom Nom", "", null, null));

        CommentaireResponse reponse = service.creerAvecPieceJointe(1L, 5L, null, fichier);

        assertThat(reponse.getPieceJointe()).isNotNull();
        assertThat(reponse.getPieceJointe().getNomFichier()).isEqualTo("cahier-des-charges.pdf");
        assertThat(reponse.getPieceJointe().getTypeContenu()).isEqualTo("application/pdf");
        verify(accesProjetService).verifierMembreDuProjet(tache.getProjet());
    }

    @Test
    void creerAvecPieceJointe_fichierVide_levePieceJointeInvalideExceptionSansEnregistrer() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "vide.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.creerAvecPieceJointe(1L, 5L, null, fichier))
                .isInstanceOf(PieceJointeInvalideException.class);

        verify(commentaireRepository, never()).save(any());
        verify(tacheRepository, never()).findById(any());
    }

    @Test
    void creerAvecPieceJointe_fichierTropVolumineux_levePieceJointeInvalideException() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "gros.pdf", "application/pdf", new byte[11 * 1024 * 1024]);

        assertThatThrownBy(() -> service.creerAvecPieceJointe(1L, 5L, null, fichier))
                .isInstanceOf(PieceJointeInvalideException.class);
    }

    @Test
    void telechargerPieceJointe_existante_laRetourne() {
        Tache tache = new Tache();
        tache.setId(1L);
        Commentaire commentaire = new Commentaire();
        commentaire.setTache(tache);
        PieceJointeCommentaire piece = new PieceJointeCommentaire();
        piece.setCommentaire(commentaire);

        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(pieceJointeCommentaireRepository.findByCommentaireId(10L)).thenReturn(Optional.of(piece));

        assertThat(service.telechargerPieceJointe(1L, 10L)).isSameAs(piece);
        verify(accesProjetService).verifierAccesLecture(tache.getProjet());
    }

    @Test
    void telechargerPieceJointe_absente_levePieceJointeIntrouvableException() {
        Tache tache = new Tache();
        tache.setId(1L);
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(pieceJointeCommentaireRepository.findByCommentaireId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.telechargerPieceJointe(1L, 10L))
                .isInstanceOf(PieceJointeIntrouvableException.class);
    }

    @Test
    void telechargerPieceJointe_commentaireDuneAutreTache_levePieceJointeIntrouvableException() {
        Tache tache = new Tache();
        tache.setId(1L);
        Tache autreTache = new Tache();
        autreTache.setId(2L);
        Commentaire commentaire = new Commentaire();
        commentaire.setTache(autreTache);
        PieceJointeCommentaire piece = new PieceJointeCommentaire();
        piece.setCommentaire(commentaire);

        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tache));
        when(pieceJointeCommentaireRepository.findByCommentaireId(10L)).thenReturn(Optional.of(piece));

        assertThatThrownBy(() -> service.telechargerPieceJointe(1L, 10L))
                .isInstanceOf(PieceJointeIntrouvableException.class);
    }
}
