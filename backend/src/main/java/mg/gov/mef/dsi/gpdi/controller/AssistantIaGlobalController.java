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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/assistant/messages")
@RequiredArgsConstructor
@Validated
@Tag(name = "Assistant IA global", description = "Assistant IA en langage libre, sur tous les projets du chef de projet connecte")
public class AssistantIaGlobalController {

    private final AssistantIaService assistantIaService;
    private final UtilisateurCourantService utilisateurCourantService;

    @GetMapping
    @Operation(summary = "Historique des echanges globaux de l'utilisateur connecte, du plus ancien au plus recent")
    public ResponseEntity<List<MessageAssistantResponse>> historique() {
        return ResponseEntity.ok(assistantIaService.historiqueGlobal());
    }

    @PostMapping
    @Operation(summary = "Poser une question libre a l'assistant IA sur l'ensemble de ses projets (peut durer plusieurs minutes)")
    public ResponseEntity<MessageAssistantResponse> poserQuestion(@Valid @RequestBody PoserQuestionRequest request) {
        Long idDemandeur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(assistantIaService.poserQuestionGlobale(request.getQuestion(), idDemandeur));
    }
}
