package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.SousTacheRequest;
import mg.gov.mef.dsi.gpdi.dto.response.SousTacheResponse;
import mg.gov.mef.dsi.gpdi.service.SousTacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/taches/{idTache}/sous-taches")
@RequiredArgsConstructor
@Validated
@Tag(name = "SousTaches", description = "Gestion des sous-taches d'une tache")
public class SousTacheController {

    private final SousTacheService sousTacheService;

    @PostMapping
    @Operation(summary = "Ajouter une sous-tache")
    public ResponseEntity<SousTacheResponse> creer(
            @PathVariable Long idTache,
            @Valid @RequestBody SousTacheRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sousTacheService.creer(idTache, request));
    }

    @GetMapping
    @Operation(summary = "Lister les sous-taches d'une tache")
    public ResponseEntity<List<SousTacheResponse>> rechercher(@PathVariable Long idTache) {
        return ResponseEntity.ok(sousTacheService.rechercher(idTache));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une sous-tache")
    public ResponseEntity<Void> supprimer(@PathVariable Long idTache, @PathVariable Long id) {
        sousTacheService.supprimer(idTache, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/basculer")
    @Operation(summary = "Basculer l'etat fait/a faire d'une sous-tache")
    public ResponseEntity<SousTacheResponse> basculer(@PathVariable Long idTache, @PathVariable Long id) {
        return ResponseEntity.ok(sousTacheService.basculer(idTache, id));
    }
}
