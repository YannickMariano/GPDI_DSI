package mg.gov.mef.dsi.gpdi.service.impl;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.client.KeycloakAdminClient;
import mg.gov.mef.dsi.gpdi.dto.response.JournalConnexionResponse;
import mg.gov.mef.dsi.gpdi.repository.UtilisateurRepository;
import mg.gov.mef.dsi.gpdi.service.JournalConnexionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Traduit les evenements bruts de l'API Admin Keycloak (LOGIN, LOGIN_ERROR, LOGOUT...) en
 * historique de connexions exploitable par le frontend. Aucune donnee n'est persistee cote GPDI :
 * Keycloak reste la seule source de verite pour ces evenements (cf. KeycloakAdminClient).
 */
@Service
@RequiredArgsConstructor
public class JournalConnexionServiceImpl implements JournalConnexionService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    public List<JournalConnexionResponse> rechercherEvenementsRecents(int max) {
        List<Map<String, Object>> evenements = keycloakAdminClient.obtenirEvenementsConnexion(max);
        List<JournalConnexionResponse> reponses = new ArrayList<>();
        for (Map<String, Object> evenement : evenements) {
            reponses.add(versReponse(evenement));
        }
        return reponses;
    }

    @SuppressWarnings("unchecked")
    private JournalConnexionResponse versReponse(Map<String, Object> evenement) {
        String type = String.valueOf(evenement.get("type"));
        Object tempsMillis = evenement.get("time");
        LocalDateTime horodatage = tempsMillis instanceof Number
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) tempsMillis).longValue()), ZoneId.systemDefault())
                : null;

        Map<String, Object> details = (Map<String, Object>) evenement.get("details");
        String matricule = details != null && details.get("username") != null
                ? String.valueOf(details.get("username")) : "";
        String nomAffiche = utilisateurRepository.findByMatricule(matricule)
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse(matricule);

        Object ip = evenement.get("ipAddress");

        return new JournalConnexionResponse(
                horodatage,
                matricule,
                nomAffiche,
                libelleEvenement(type),
                ip != null ? String.valueOf(ip) : "",
                resultat(type));
    }

    private String libelleEvenement(String type) {
        if ("LOGIN".equals(type)) return "Connexion — authentification par identifiant";
        if ("LOGIN_ERROR".equals(type)) return "Tentative de connexion — identifiants invalides";
        if ("LOGOUT".equals(type)) return "Déconnexion";
        if ("LOGOUT_ERROR".equals(type)) return "Erreur lors de la déconnexion";
        return type;
    }

    private String resultat(String type) {
        if ("LOGIN_ERROR".equals(type) || "LOGOUT_ERROR".equals(type)) return "echec";
        return "succes";
    }
}
