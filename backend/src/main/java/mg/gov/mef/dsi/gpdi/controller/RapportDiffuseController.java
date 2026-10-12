package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.RapportDiffuseRequest;
import mg.gov.mef.dsi.gpdi.dto.response.RapportDiffuseResponse;
import mg.gov.mef.dsi.gpdi.service.RapportDiffuseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/rapports-diffuses")
@RequiredArgsConstructor
@Validated
@Tag(name = "Rapports diffusés", description = "Rapports diffusés par les chefs de projet aux membres concernés et au Directeur DSI")
public class RapportDiffuseController {

    private final RapportDiffuseService rapportDiffuseService;

    @PostMapping
    @Operation(summary = "Diffuser un rapport (photo figee) sur un projet (reserve au chef de projet responsable)")
    public ResponseEntity<RapportDiffuseResponse> diffuser(@Valid @RequestBody RapportDiffuseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rapportDiffuseService.diffuser(request));
    }

    @GetMapping
    @Operation(summary = "Rapports diffuses visibles par l'utilisateur courant, du plus recent au plus ancien "
            + "(tous pour le Directeur DSI, sinon ceux des projets dont il est membre concerne)")
    public ResponseEntity<List<RapportDiffuseResponse>> rechercher() {
        return ResponseEntity.ok(rapportDiffuseService.rechercherPourUtilisateurCourant());
    }
}
