package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.request.PoserQuestionRequest;
import mg.gov.mef.dsi.gpdi.dto.response.MessageAssistantResponse;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.AssistantIaService;
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
@RequestMapping("/api/projets/{idProjet}/assistant/messages")
@RequiredArgsConstructor
@Validated
@Tag(name = "Assistant IA", description = "Assistant IA en langage libre, sur le contexte du projet (reserve au chef de projet)")
public class AssistantIaController {

    private final AssistantIaService assistantIaService;
    private final UtilisateurCourantService utilisateurCourantService;

    @GetMapping
    @Operation(summary = "Historique des echanges avec l'assistant IA du projet, du plus ancien au plus recent")
    public ResponseEntity<List<MessageAssistantResponse>> historique(@PathVariable Long idProjet) {
        return ResponseEntity.ok(assistantIaService.historique(idProjet));
    }

    @PostMapping
    @Operation(summary = "Poser une question libre a l'assistant IA sur le projet (peut durer plusieurs minutes)")
    public ResponseEntity<MessageAssistantResponse> poserQuestion(@PathVariable Long idProjet,
                                                                   @Valid @RequestBody PoserQuestionRequest request) {
        Long idDemandeur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(assistantIaService.poserQuestion(idProjet, request.getQuestion(), idDemandeur));
    }
}
