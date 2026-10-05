package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.SprintRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SprintResponse;
import mg.gov.mef.dsi.gpdi.service.SprintService;
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
@RequestMapping("/api/projets/{idProjet}/sprints")
@RequiredArgsConstructor
@Validated
@Tag(name = "Sprints", description = "Gestion des sprints d'un projet")
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    @Operation(summary = "Creer un sprint dans un projet")
    public ResponseEntity<SprintResponse> creer(
            @PathVariable Long idProjet,
            @Valid @RequestBody SprintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sprintService.creer(idProjet, request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les sprints d'un projet de maniere paginee")
    public ResponseEntity<Page<SprintResponse>> rechercher(@PathVariable Long idProjet, Pageable pageable) {
        return ResponseEntity.ok(sprintService.rechercher(idProjet, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un sprint par son identifiant")
    public ResponseEntity<SprintResponse> obtenir(@PathVariable Long idProjet, @PathVariable Long id) {
        return ResponseEntity.ok(sprintService.obtenir(idProjet, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un sprint existant")
    public ResponseEntity<SprintResponse> modifier(
            @PathVariable Long idProjet,
            @PathVariable Long id,
            @Valid @RequestBody SprintRequest request) {
        return ResponseEntity.ok(sprintService.modifier(idProjet, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un sprint")
    public ResponseEntity<Void> supprimer(@PathVariable Long idProjet, @PathVariable Long id) {
        sprintService.supprimer(idProjet, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/commencer")
    @Operation(summary = "Commencer un sprint (PLANIFIE -> EN_COURS)")
    public ResponseEntity<SprintResponse> commencer(@PathVariable Long idProjet, @PathVariable Long id) {
        return ResponseEntity.ok(sprintService.commencer(idProjet, id));
    }

    @PostMapping("/{id}/terminer")
    @Operation(summary = "Terminer un sprint (EN_COURS -> CLOTURER)")
    public ResponseEntity<SprintResponse> terminer(@PathVariable Long idProjet, @PathVariable Long id) {
        return ResponseEntity.ok(sprintService.terminer(idProjet, id));
    }
}
