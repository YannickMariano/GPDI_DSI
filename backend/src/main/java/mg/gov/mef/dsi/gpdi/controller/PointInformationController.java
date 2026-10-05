package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.PointInformationRequest;
import mg.gov.mef.dsi.gpdi.dto.response.PointInformationResponse;
import mg.gov.mef.dsi.gpdi.service.PointInformationService;
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

@RestController
@RequestMapping("/api/projets/{idProjet}/points-information")
@RequiredArgsConstructor
@Validated
@Tag(name = "Points d'information", description = "Points d'information publiés par le chef de projet à destination des comptes clients")
public class PointInformationController {

    private final PointInformationService pointInformationService;

    @PostMapping
    @Operation(summary = "Publier un point d'information sur un projet (reserve au chef de projet)")
    public ResponseEntity<PointInformationResponse> creer(
            @PathVariable Long idProjet,
            @Valid @RequestBody PointInformationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pointInformationService.creer(idProjet, request));
    }

    @GetMapping
    @Operation(summary = "Rechercher les points d'information d'un projet, du plus recent au plus ancien "
            + "(reserve au chef de projet ou a un compte client avec un partage actif sur ce projet)")
    public ResponseEntity<Page<PointInformationResponse>> rechercher(@PathVariable Long idProjet, Pageable pageable) {
        return ResponseEntity.ok(pointInformationService.rechercher(idProjet, pageable));
    }
}
