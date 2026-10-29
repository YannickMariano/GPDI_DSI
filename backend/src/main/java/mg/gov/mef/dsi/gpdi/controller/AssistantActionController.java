package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.ActionAssistantResponse;
import mg.gov.mef.dsi.gpdi.service.ActionAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant/actions")
@RequiredArgsConstructor
@Tag(name = "Actions de l'assistant IA", description = "Confirmation ou annulation des actions proposees par l'assistant IA")
public class AssistantActionController {

    private final ActionAssistantService actionAssistantService;

    @PostMapping("/{id}/confirmer")
    @Operation(summary = "Confirmer et executer une action proposee par l'assistant IA")
    public ResponseEntity<ActionAssistantResponse> confirmer(@PathVariable Long id) {
        return ResponseEntity.ok(actionAssistantService.confirmer(id));
    }

    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annuler une action proposee par l'assistant IA (rien n'est modifie)")
    public ResponseEntity<ActionAssistantResponse> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(actionAssistantService.annuler(id));
    }
}
