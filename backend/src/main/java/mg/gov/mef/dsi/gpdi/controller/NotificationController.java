package mg.gov.mef.dsi.gpdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.dto.response.NotificationResponse;
import mg.gov.mef.dsi.gpdi.security.UtilisateurCourantService;
import mg.gov.mef.dsi.gpdi.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications personnelles de l'utilisateur connecte")
public class NotificationController {

    private final NotificationService notificationService;
    private final UtilisateurCourantService utilisateurCourantService;

    @GetMapping
    @Operation(summary = "Lister mes notifications, les plus recentes d'abord")
    public ResponseEntity<Page<NotificationResponse>> rechercher(Pageable pageable) {
        Long idUtilisateur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(notificationService.rechercherPourUtilisateur(idUtilisateur, pageable));
    }

    @PostMapping("/{id}/lire")
    @Operation(summary = "Marquer l'une de mes notifications comme lue")
    public ResponseEntity<NotificationResponse> lire(@PathVariable Long id) {
        Long idUtilisateur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        return ResponseEntity.ok(notificationService.marquerCommeLue(idUtilisateur, id));
    }

    @PostMapping("/lire-toutes")
    @Operation(summary = "Marquer toutes mes notifications comme lues")
    public ResponseEntity<Void> lireToutes() {
        Long idUtilisateur = utilisateurCourantService.obtenirUtilisateurCourant().getId();
        notificationService.marquerToutesCommeLues(idUtilisateur);
        return ResponseEntity.noContent().build();
    }
}
