package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.TacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TacheDetailResponse;
import mg.gov.mef.dsi.gpdi.dto.response.TacheResponse;
import mg.gov.mef.dsi.gpdi.service.TacheService;
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
@RequestMapping("/api/projets/{idProjet}/taches")
@RequiredArgsConstructor
@Validated
@Tag(name = "Taches", description = "Gestion des taches d'un projet")
public class TacheController {

    private final TacheService tacheService;

    @PostMapping
    @Operation(summary = "Creer une tache dans un projet")
    public ResponseEntity<TacheResponse> creer(
            @PathVariable Long idProjet,
            @Valid @RequestBody TacheRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tacheService.creer(idProjet, request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les taches d'un projet de maniere paginee")
    public ResponseEntity<Page<TacheResponse>> rechercher(@PathVariable Long idProjet, Pageable pageable) {
        return ResponseEntity.ok(tacheService.rechercher(idProjet, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le detail complet d'une tache")
    public ResponseEntity<TacheDetailResponse> obtenir(@PathVariable Long idProjet, @PathVariable Long id) {
        return ResponseEntity.ok(tacheService.obtenir(idProjet, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une tache existante")
    public ResponseEntity<TacheResponse> modifier(
            @PathVariable Long idProjet,
            @PathVariable Long id,
            @Valid @RequestBody TacheRequest request) {
        return ResponseEntity.ok(tacheService.modifier(idProjet, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une tache")
    public ResponseEntity<Void> supprimer(@PathVariable Long idProjet, @PathVariable Long id) {
        tacheService.supprimer(idProjet, id);
        return ResponseEntity.noContent().build();
    }
}
