package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.CommentaireRequest;
import mg.gov.mef.dsi.gpdi.dto.response.CommentaireResponse;
import mg.gov.mef.dsi.gpdi.entity.PieceJointeCommentaire;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.CommentaireService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/taches/{idTache}/commentaires")
@RequiredArgsConstructor
@Validated
@Tag(name = "Commentaires", description = "Activite (commentaires) d'une tache")
public class CommentaireController {

    private final CommentaireService commentaireService;
    private final UtilisateurCourantService utilisateurCourantService;

    @PostMapping
    @Operation(summary = "Ajouter un commentaire a une tache (auteur deduit du jeton connecte)")
    public ResponseEntity<CommentaireResponse> creer(
            @PathVariable Long idTache,
            @Valid @RequestBody CommentaireRequest request) {
        Long idAuteur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(commentaireService.creer(idTache, idAuteur, request));
    }

    @PostMapping(path = "/avec-piece-jointe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter un commentaire avec une piece jointe (contenu texte facultatif)")
    public ResponseEntity<CommentaireResponse> creerAvecPieceJointe(
            @PathVariable Long idTache,
            @RequestParam(required = false) String contenu,
            @RequestPart MultipartFile fichier) {
        Long idAuteur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentaireService.creerAvecPieceJointe(idTache, idAuteur, contenu, fichier));
    }

    @GetMapping
    @Operation(summary = "Lister les commentaires d'une tache par ordre chronologique")
    public ResponseEntity<List<CommentaireResponse>> rechercher(@PathVariable Long idTache) {
        return ResponseEntity.ok(commentaireService.rechercher(idTache));
    }

    @GetMapping("/{idCommentaire}/piece-jointe")
    @Operation(summary = "Telecharger la piece jointe d'un commentaire")
    public ResponseEntity<byte[]> telechargerPieceJointe(@PathVariable Long idTache, @PathVariable Long idCommentaire) {
        PieceJointeCommentaire piece = commentaireService.telechargerPieceJointe(idTache, idCommentaire);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(piece.getTypeContenu()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(piece.getNomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(piece.getContenu());
    }
}
