package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.JournalConnexionResponse;
import mg.gov.mef.dsi.gpdi.service.JournalConnexionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/journal-connexions")
@RequiredArgsConstructor
@Tag(name = "JournalConnexions", description = "Historique des connexions, base sur les evenements Keycloak")
public class JournalConnexionController {

    private final JournalConnexionService journalConnexionService;

    @GetMapping
    @Operation(summary = "Lister les derniers evenements de connexion (Keycloak)")
    public ResponseEntity<List<JournalConnexionResponse>> rechercher(@RequestParam(defaultValue = "100") int max) {
        return ResponseEntity.ok(journalConnexionService.rechercherEvenementsRecents(max));
    }
}
