package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.BurndownPointResponse;
import mg.gov.mef.dsi.gpdi.service.BurndownService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projets/{idProjet}/sprints/{idSprint}/burndown")
@RequiredArgsConstructor
@Tag(name = "Burndown", description = "Courbe de burndown (reel vs ideal) d'un sprint")
public class BurndownController {

    private final BurndownService burndownService;

    @GetMapping
    @Operation(summary = "Obtenir la courbe de burndown d'un sprint, jour par jour")
    public ResponseEntity<List<BurndownPointResponse>> obtenir(@PathVariable Long idProjet, @PathVariable Long idSprint) {
        return ResponseEntity.ok(burndownService.obtenir(idProjet, idSprint));
    }
}
