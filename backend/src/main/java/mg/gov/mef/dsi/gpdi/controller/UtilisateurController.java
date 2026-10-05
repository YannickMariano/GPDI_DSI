package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurCreationRequest;
import mg.gov.mef.dsi.gpdi.dto.request.UtilisateurModificationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.UtilisateurResponse;
import mg.gov.mef.dsi.gpdi.service.UtilisateurService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs applicatifs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @PostMapping
    @Operation(summary = "Creer un utilisateur")
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody UtilisateurCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurService.creer(request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les utilisateurs de maniere paginee")
    public ResponseEntity<Page<UtilisateurResponse>> rechercher(Pageable pageable) {
        return ResponseEntity.ok(utilisateurService.rechercher(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un utilisateur par son identifiant")
    public ResponseEntity<UtilisateurResponse> obtenir(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.obtenir(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un utilisateur existant")
    public ResponseEntity<UtilisateurResponse> modifier(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurModificationRequest request) {
        return ResponseEntity.ok(utilisateurService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un utilisateur (409 s'il est encore assigne a des taches, sauf detacherTaches=true)")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean detacherTaches) {
        utilisateurService.supprimer(id, detacherTaches);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/moi")
    @Operation(summary = "Obtenir l'utilisateur actuellement authentifie")
    public ResponseEntity<UtilisateurResponse> obtenirCourant() {
        return ResponseEntity.ok(utilisateurService.obtenirCourant());
    }

    @PostMapping("/{id}/activer")
    @Operation(summary = "Activer un utilisateur")
    public ResponseEntity<UtilisateurResponse> activer(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.activer(id));
    }

    @PostMapping("/{id}/suspendre")
    @Operation(summary = "Suspendre un utilisateur")
    public ResponseEntity<UtilisateurResponse> suspendre(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.suspendre(id));
    }
}
