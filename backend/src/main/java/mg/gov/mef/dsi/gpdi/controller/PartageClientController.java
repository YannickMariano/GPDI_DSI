package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.PartageClientRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PartageClientResponse;
import mg.gov.mef.dsi.gpdi.service.PartageClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/partages-clients")
@RequiredArgsConstructor
@Validated
@Tag(name = "PartagesClients", description = "Partage de projets avec des comptes clients")
public class PartageClientController {

    private final PartageClientService partageClientService;

    @PostMapping
    @Operation(summary = "Partager un ou plusieurs projets avec un compte client")
    public ResponseEntity<List<PartageClientResponse>> creer(@Valid @RequestBody PartageClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partageClientService.creer(request));
    }

    @GetMapping
    @Operation(summary = "Rechercher tous les partages de maniere paginee")
    public ResponseEntity<Page<PartageClientResponse>> rechercher(Pageable pageable) {
        return ResponseEntity.ok(partageClientService.rechercher(pageable));
    }

    @PostMapping("/{id}/revoquer")
    @Operation(summary = "Revoquer un partage actif")
    public ResponseEntity<PartageClientResponse> revoquer(@PathVariable Long id) {
        return ResponseEntity.ok(partageClientService.revoquer(id));
    }
}
