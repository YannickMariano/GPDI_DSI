package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.CommentaireRequest;
import mg.gov.mef.dsi.gpdi.dto.response.CommentaireResponse;
import mg.gov.mef.dsi.gpdi.dto.response.PieceJointeResponse;
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
import mg.gov.mef.dsi.gpdi.service.CommentaireService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentaireServiceImpl implements CommentaireService {

    private static final Logger log = LoggerFactory.getLogger(CommentaireServiceImpl.class);

    /** Coherent avec spring.servlet.multipart.max-file-size (application.properties). */
    private static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024;

    private final CommentaireRepository commentaireRepository;
    private final TacheRepository tacheRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TacheMapper tacheMapper;
    private final AccesProjetService accesProjetService;
    private final PieceJointeCommentaireRepository pieceJointeCommentaireRepository;

    @Override
    @Transactional
    public CommentaireResponse creer(Long idTache, Long idAuteur, CommentaireRequest request) {
        Tache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
        accesProjetService.verifierMembreDuProjet(tache.getProjet());
        Utilisateur auteur = utilisateurRepository.findById(idAuteur)
                .orElseThrow(() -> new UtilisateurIntrouvableException(idAuteur));

        Commentaire commentaire = new Commentaire();
        commentaire.setTache(tache);
        commentaire.setAuteur(auteur);
        commentaire.setContenu(request.getContenu());

        CommentaireResponse reponse = tacheMapper.toCommentaireResponse(commentaireRepository.save(commentaire));
        log.info("Commentaire {} ajoute a la tache {}", reponse.getId(), idTache);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentaireResponse> rechercher(Long idTache) {
        Tache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
        accesProjetService.verifierAccesLecture(tache.getProjet());

        List<Commentaire> commentaires = commentaireRepository.findByTacheIdOrderByDateCreationAsc(idTache);
        List<Long> idsCommentaires = commentaires.stream().map(Commentaire::getId).collect(Collectors.toList());
        Map<Long, PieceJointeResponse> piecesJointesParCommentaire = new HashMap<>();
        for (PieceJointeCommentaire piece : pieceJointeCommentaireRepository.findByCommentaireIdIn(idsCommentaires)) {
            piecesJointesParCommentaire.put(piece.getCommentaire().getId(), versPieceJointeResponse(piece));
        }

        return commentaires.stream()
                .map(commentaire -> {
                    CommentaireResponse reponse = tacheMapper.toCommentaireResponse(commentaire);
                    reponse.setPieceJointe(piecesJointesParCommentaire.get(commentaire.getId()));
                    return reponse;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentaireResponse creerAvecPieceJointe(Long idTache, Long idAuteur, String contenu, MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new PieceJointeInvalideException("Le fichier joint est vide ou manquant.");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new PieceJointeInvalideException("Le fichier depasse la taille maximale autorisee (10 Mo).");
        }

        Tache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
        accesProjetService.verifierMembreDuProjet(tache.getProjet());
        Utilisateur auteur = utilisateurRepository.findById(idAuteur)
                .orElseThrow(() -> new UtilisateurIntrouvableException(idAuteur));

        Commentaire commentaire = new Commentaire();
        commentaire.setTache(tache);
        commentaire.setAuteur(auteur);
        commentaire.setContenu(StringUtils.hasText(contenu) ? contenu : "");
        commentaire = commentaireRepository.save(commentaire);

        PieceJointeCommentaire piece = new PieceJointeCommentaire();
        piece.setCommentaire(commentaire);
        piece.setNomFichier(StringUtils.hasText(fichier.getOriginalFilename()) ? fichier.getOriginalFilename() : "fichier");
        piece.setTypeContenu(StringUtils.hasText(fichier.getContentType()) ? fichier.getContentType() : "application/octet-stream");
        piece.setTaille(fichier.getSize());
        try {
            piece.setContenu(fichier.getBytes());
        } catch (IOException e) {
            throw new PieceJointeInvalideException("Impossible de lire le contenu du fichier joint.");
        }
        piece = pieceJointeCommentaireRepository.save(piece);

        CommentaireResponse reponse = tacheMapper.toCommentaireResponse(commentaire);
        reponse.setPieceJointe(versPieceJointeResponse(piece));
        log.info("Commentaire {} avec piece jointe « {} » ajoute a la tache {}", reponse.getId(), piece.getNomFichier(), idTache);
        return reponse;
    }

    @Override
    @Transactional(readOnly = true)
    public PieceJointeCommentaire telechargerPieceJointe(Long idTache, Long idCommentaire) {
        Tache tache = tacheRepository.findById(idTache)
                .orElseThrow(() -> new TacheIntrouvableException(idTache));
        accesProjetService.verifierAccesLecture(tache.getProjet());

        return pieceJointeCommentaireRepository.findByCommentaireId(idCommentaire)
                .filter(piece -> piece.getCommentaire().getTache() != null
                        && piece.getCommentaire().getTache().getId().equals(idTache))
                .orElseThrow(() -> new PieceJointeIntrouvableException(idCommentaire));
    }

    private PieceJointeResponse versPieceJointeResponse(PieceJointeCommentaire piece) {
        return new PieceJointeResponse(piece.getId(), piece.getNomFichier(), piece.getTypeContenu(), piece.getTaille());
    }
}
