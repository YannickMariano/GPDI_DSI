package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.EquipeRequest;
import mg.gov.mef.dsi.gpdi.dto.response.EquipeResponse;
import mg.gov.mef.dsi.gpdi.service.EquipeService;
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
@RequestMapping("/api/equipes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Equipes", description = "Gestion des equipes et de leurs membres")
public class EquipeController {

    private final EquipeService equipeService;

    @PostMapping
    @Operation(summary = "Creer une equipe")
    public ResponseEntity<EquipeResponse> creer(@Valid @RequestBody EquipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.creer(request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les equipes de maniere paginee")
    public ResponseEntity<Page<EquipeResponse>> rechercher(Pageable pageable) {
        return ResponseEntity.ok(equipeService.rechercher(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une equipe par son identifiant")
    public ResponseEntity<EquipeResponse> obtenir(@PathVariable Long id) {
        return ResponseEntity.ok(equipeService.obtenir(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une equipe existante")
    public ResponseEntity<EquipeResponse> modifier(
            @PathVariable Long id,
            @Valid @RequestBody EquipeRequest request) {
        return ResponseEntity.ok(equipeService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une equipe")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        equipeService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
