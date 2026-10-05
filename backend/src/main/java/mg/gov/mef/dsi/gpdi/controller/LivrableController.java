package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRejetRequest;
import mg.gov.mef.dsi.gpdi.dto.request.LivrableRequest;
import mg.gov.mef.dsi.gpdi.dto.response.LivrableResponse;
import mg.gov.mef.dsi.gpdi.service.LivrableService;
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
@RequestMapping("/api/jalons/{idJalon}/livrables")
@RequiredArgsConstructor
@Validated
@Tag(name = "Livrables", description = "Gestion des livrables d'un jalon")
public class LivrableController {

    private final LivrableService livrableService;

    @PostMapping
    @Operation(summary = "Creer un livrable dans un jalon")
    public ResponseEntity<LivrableResponse> creer(
            @PathVariable Long idJalon,
            @Valid @RequestBody LivrableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(livrableService.creer(idJalon, request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les livrables d'un jalon de maniere paginee")
    public ResponseEntity<Page<LivrableResponse>> rechercher(@PathVariable Long idJalon, Pageable pageable) {
        return ResponseEntity.ok(livrableService.rechercher(idJalon, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un livrable par son identifiant")
    public ResponseEntity<LivrableResponse> obtenir(@PathVariable Long idJalon, @PathVariable Long id) {
        return ResponseEntity.ok(livrableService.obtenir(idJalon, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un livrable existant")
    public ResponseEntity<LivrableResponse> modifier(
            @PathVariable Long idJalon,
            @PathVariable Long id,
            @Valid @RequestBody LivrableRequest request) {
        return ResponseEntity.ok(livrableService.modifier(idJalon, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un livrable")
    public ResponseEntity<Void> supprimer(@PathVariable Long idJalon, @PathVariable Long id) {
        livrableService.supprimer(idJalon, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/livrer")
    @Operation(summary = "Marquer un livrable comme livre")
    public ResponseEntity<LivrableResponse> livrer(@PathVariable Long idJalon, @PathVariable Long id) {
        return ResponseEntity.ok(livrableService.livrer(idJalon, id));
    }

    @PostMapping("/{id}/rejeter")
    @Operation(summary = "Rejeter un livrable avec un motif")
    public ResponseEntity<LivrableResponse> rejeter(
            @PathVariable Long idJalon,
            @PathVariable Long id,
            @Valid @RequestBody LivrableRejetRequest request) {
        return ResponseEntity.ok(livrableService.rejeter(idJalon, id, request));
    }
}
