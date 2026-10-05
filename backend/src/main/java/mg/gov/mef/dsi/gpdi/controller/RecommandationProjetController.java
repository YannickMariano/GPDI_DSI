package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.RecommandationProjetResponse;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.RecommandationProjetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projets/{idProjet}/recommandations")
@RequiredArgsConstructor
@Tag(name = "Recommandations", description = "Recommandations proactives de pilotage de projet (reserve au chef de projet)")
public class RecommandationProjetController {

    private final RecommandationProjetService recommandationProjetService;
    private final UtilisateurCourantService utilisateurCourantService;

    @GetMapping
    @Operation(summary = "Lister les recommandations en attente du projet, de la plus recente a la plus ancienne")
    public ResponseEntity<List<RecommandationProjetResponse>> lister(@PathVariable Long idProjet) {
        return ResponseEntity.ok(recommandationProjetService.lister(idProjet));
    }

    @PostMapping("/analyser")
    @Operation(summary = "Analyser le projet et proposer de nouvelles recommandations (peut durer plusieurs minutes)")
    public ResponseEntity<List<RecommandationProjetResponse>> analyser(@PathVariable Long idProjet) {
        Long idDemandeur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(recommandationProjetService.analyser(idProjet, idDemandeur));
    }

    @PostMapping("/{id}/appliquer")
    @Operation(summary = "Appliquer une recommandation en attente : execute l'action proposee sur le projet")
    public ResponseEntity<RecommandationProjetResponse> appliquer(@PathVariable Long idProjet, @PathVariable Long id) {
        Long idValidateur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(recommandationProjetService.appliquer(idProjet, id, idValidateur));
    }

    @PostMapping("/{id}/rejeter")
    @Operation(summary = "Rejeter une recommandation en attente sans l'executer")
    public ResponseEntity<RecommandationProjetResponse> rejeter(@PathVariable Long idProjet, @PathVariable Long id) {
        Long idValidateur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(recommandationProjetService.rejeter(idProjet, id, idValidateur));
    }
}
