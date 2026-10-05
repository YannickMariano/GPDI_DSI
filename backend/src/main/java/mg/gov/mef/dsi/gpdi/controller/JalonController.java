package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.JalonRequest;
import mg.gov.mef.dsi.gpdi.dto.response.JalonResponse;
import mg.gov.mef.dsi.gpdi.service.JalonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/projets/{idProjet}/jalons")
@RequiredArgsConstructor
@Validated
@Tag(name = "Jalons", description = "Gestion des jalons d'un projet")
public class JalonController {

    private final JalonService jalonService;

    @PostMapping
    @Operation(summary = "Creer un jalon dans un projet")
    public ResponseEntity<JalonResponse> creer(
            @PathVariable Long idProjet,
            @Valid @RequestBody JalonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jalonService.creer(idProjet, request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les jalons d'un projet de maniere paginee")
    public ResponseEntity<Page<JalonResponse>> rechercher(@PathVariable Long idProjet, Pageable pageable) {
        return ResponseEntity.ok(jalonService.rechercher(idProjet, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un jalon par son identifiant")
    public ResponseEntity<JalonResponse> obtenir(@PathVariable Long idProjet, @PathVariable Long id) {
        return ResponseEntity.ok(jalonService.obtenir(idProjet, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un jalon existant")
    public ResponseEntity<JalonResponse> modifier(
            @PathVariable Long idProjet,
            @PathVariable Long id,
            @Valid @RequestBody JalonRequest request) {
        return ResponseEntity.ok(jalonService.modifier(idProjet, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un jalon")
    public ResponseEntity<Void> supprimer(@PathVariable Long idProjet, @PathVariable Long id) {
        jalonService.supprimer(idProjet, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/terminer")
    @Operation(summary = "Marquer un jalon comme atteint")
    public ResponseEntity<JalonResponse> terminer(@PathVariable Long idProjet, @PathVariable Long id) {
        return ResponseEntity.ok(jalonService.terminer(idProjet, id));
    }

    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annuler un jalon")
    public ResponseEntity<JalonResponse> annuler(@PathVariable Long idProjet, @PathVariable Long id) {
        return ResponseEntity.ok(jalonService.annuler(idProjet, id));
    }
}
