package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.RoleRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RoleResponse;
import mg.gov.mef.dsi.gpdi.service.RoleService;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Validated
@Tag(name = "Roles", description = "Gestion des roles applicatifs")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @Operation(summary = "Creer un role")
    public ResponseEntity<RoleResponse> creer(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.creer(request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les roles de maniere paginee")
    public ResponseEntity<Page<RoleResponse>> rechercher(Pageable pageable) {
        return ResponseEntity.ok(roleService.rechercher(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un role par son identifiant")
    public ResponseEntity<RoleResponse> obtenir(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.obtenir(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un role existant")
    public ResponseEntity<RoleResponse> modifier(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un role")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        roleService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}