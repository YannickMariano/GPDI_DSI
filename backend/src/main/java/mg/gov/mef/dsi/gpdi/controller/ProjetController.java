package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.ProjetRequest;
import mg.gov.mef.dsi.gpdi.dto.response.ProjetResponse;
import mg.gov.mef.dsi.gpdi.service.ProjetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
@Validated
@Tag(name = "Projets", description = "Gestion des projets")
public class ProjetController {

    private final ProjetService projetService;

    @PostMapping
    @Operation(summary = "Creer un projet")
    public ResponseEntity<ProjetResponse> creer(@Valid @RequestBody ProjetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.creer(request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les projets de maniere paginee")
    public ResponseEntity<Page<ProjetResponse>> rechercher(Pageable pageable) {
        return ResponseEntity.ok(projetService.rechercher(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un projet par son identifiant")
    public ResponseEntity<ProjetResponse> obtenir(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.obtenir(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un projet existant")
    public ResponseEntity<ProjetResponse> modifier(
            @PathVariable Long id,
            @Valid @RequestBody ProjetRequest request) {
        return ResponseEntity.ok(projetService.modifier(id, request));
    }

    @PostMapping("/{id}/demarrer")
    @Operation(summary = "Demarrer manuellement un projet EN_PREPARATION (statut EN_COURS)")
    public ResponseEntity<ProjetResponse> demarrer(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.demarrer(id));
    }

    @PostMapping("/{id}/suspendre")
    @Operation(summary = "Suspendre un projet (statut SUSPENDU)")
    public ResponseEntity<ProjetResponse> suspendre(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.suspendre(id));
    }

    @PostMapping("/{id}/reprendre")
    @Operation(summary = "Reprendre un projet SUSPENDU (statut EN_COURS)")
    public ResponseEntity<ProjetResponse> reprendre(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.reprendre(id));
    }

    @PostMapping("/{id}/cloturer")
    @Operation(summary = "Cloturer un projet (statut CLOTURE, date de cloture fixee a aujourd'hui)")
    public ResponseEntity<ProjetResponse> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.cloturer(id));
    }

    @PostMapping("/{id}/archiver")
    @Operation(summary = "Archiver un projet (remplace la suppression, statut ARCHIVE)")
    public ResponseEntity<ProjetResponse> archiver(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.archiver(id));
    }
}
