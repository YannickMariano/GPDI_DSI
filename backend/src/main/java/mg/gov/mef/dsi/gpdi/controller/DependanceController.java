package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.DependanceRequest;
import mg.gov.mef.dsi.gpdi.dto.response.TachePrecedenceResponse;
import mg.gov.mef.dsi.gpdi.service.DependanceService;
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
@RequestMapping("/api/taches/{idTache}/dependances")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dependances", description = "Dependances entre taches")
public class DependanceController {

    private final DependanceService dependanceService;

    @PostMapping
    @Operation(summary = "Ajouter une dependance depuis cette tache vers une autre")
    public ResponseEntity<TachePrecedenceResponse> creer(
            @PathVariable Long idTache,
            @Valid @RequestBody DependanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dependanceService.creer(idTache, request));
    }

    @GetMapping
    @Operation(summary = "Lister les dependances d'une tache")
    public ResponseEntity<List<TachePrecedenceResponse>> rechercher(@PathVariable Long idTache) {
        return ResponseEntity.ok(dependanceService.rechercher(idTache));
    }

    @DeleteMapping("/{idAutreTache}")
    @Operation(summary = "Supprimer la dependance entre cette tache et une autre")
    public ResponseEntity<Void> supprimer(@PathVariable Long idTache, @PathVariable Long idAutreTache) {
        dependanceService.supprimer(idTache, idAutreTache);
        return ResponseEntity.noContent().build();
    }
}
