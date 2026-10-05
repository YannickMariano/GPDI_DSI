package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.HabilitationLigneRequest;
import mg.gov.mef.dsi.gpdi.dto.response.HabilitationResponse;
import mg.gov.mef.dsi.gpdi.service.HabilitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/habilitations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Habilitations", description = "Matrice d'habilitations par role metier et par fonction")
public class HabilitationController {

    private final HabilitationService habilitationService;

    @GetMapping
    @Operation(summary = "Consulter la matrice d'habilitations")
    public ResponseEntity<List<HabilitationResponse>> rechercher() {
        return ResponseEntity.ok(habilitationService.rechercherMatrice());
    }

    @PutMapping
    @Operation(summary = "Mettre a jour la matrice d'habilitations (cases modifiees)")
    public ResponseEntity<List<HabilitationResponse>> mettreAJour(@Valid @RequestBody List<HabilitationLigneRequest> lignes) {
        habilitationService.mettreAJourMatrice(lignes);
        return ResponseEntity.ok(habilitationService.rechercherMatrice());
    }
}
