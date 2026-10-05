package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.AppliquerSolutionsRequest;
import mg.gov.mef.dsi.gpdi.dto.request.RejeterRisqueRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ResultatDetectionRisqueResponse;
import mg.gov.mef.dsi.gpdi.dto.response.RisqueResponse;
import mg.gov.mef.dsi.gpdi.dto.response.SolutionRisqueResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TraitementRisqueResponse;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.DetectionRisqueService;
import mg.gov.mef.dsi.gpdi.service.RisqueService;
import mg.gov.mef.dsi.gpdi.service.SuggestionRisqueService;
import mg.gov.mef.dsi.gpdi.service.TraitementRisqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/projets/{idProjet}/risques")
@RequiredArgsConstructor
@Tag(name = "Risques", description = "Gestion des risques d'un projet assistee par l'IA")
public class RisqueController {

    private final RisqueService risqueService;
    private final DetectionRisqueService detectionRisqueService;
    private final SuggestionRisqueService suggestionRisqueService;
    private final TraitementRisqueService traitementRisqueService;
    private final UtilisateurCourantService utilisateurCourantService;

    @GetMapping
    @Operation(summary = "Lister tous les risques du projet (R1, R2... par ordre de detection) avec leurs solutions en attente")
    public ResponseEntity<List<RisqueResponse>> rechercher(@PathVariable Long idProjet) {
        return ResponseEntity.ok(risqueService.rechercher(idProjet));
    }

    @PostMapping("/detection")
    @Operation(summary = "Detecter les risques du projet avec l'assistant IA (peut durer plusieurs minutes)")
    public ResponseEntity<ResultatDetectionRisqueResponse> detecter(@PathVariable Long idProjet) {
        Long idDemandeur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(detectionRisqueService.detecter(idProjet, idDemandeur));
    }

    @PostMapping("/{idRisque}/suggestions")
    @Operation(summary = "Obtenir les solutions proposees par l'assistant IA pour un risque ouvert (deja calculees si elles existent, sinon peut durer plusieurs minutes)")
    public ResponseEntity<List<SolutionRisqueResponse>> suggerer(@PathVariable Long idProjet, @PathVariable Long idRisque) {
        Long idDemandeur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(suggestionRisqueService.suggerer(idProjet, idRisque, idDemandeur));
    }

    @PostMapping("/{idRisque}/appliquer")
    @Operation(summary = "Appliquer sur le projet les solutions cochees d'un risque ouvert (tout ou rien) ; le risque passe a SOUS_CONTROLE")
    public ResponseEntity<TraitementRisqueResponse> appliquer(@PathVariable Long idProjet, @PathVariable Long idRisque,
                                                              @Valid @RequestBody AppliquerSolutionsRequest request) {
        Long idValidateur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(traitementRisqueService.appliquer(idProjet, idRisque, request.getIdsSolutions(), idValidateur));
    }

    @PostMapping("/{idRisque}/rejeter")
    @Operation(summary = "Rejeter un risque ouvert : il passe a REJETE et ses solutions en attente sont rejetees")
    public ResponseEntity<TraitementRisqueResponse> rejeter(@PathVariable Long idProjet, @PathVariable Long idRisque,
                                                            @Valid @RequestBody(required = false) RejeterRisqueRequest request) {
        Long idValidateur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        String motif = request == null ? null : request.getMotif();
        return ResponseEntity.ok(traitementRisqueService.rejeter(idProjet, idRisque, motif, idValidateur));
    }
}
