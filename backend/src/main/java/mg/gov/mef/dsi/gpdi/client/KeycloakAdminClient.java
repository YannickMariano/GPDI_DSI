package mg.gov.mef.dsi.gpdi.client;

import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.exception.KeycloakIndisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client d'administration Keycloak (creation/suppression de comptes), via un client technique
 * dedie en flux Client Credentials — distinct du client applicatif (gpdi-client-id) utilise pour
 * l'authentification des utilisateurs (cf. §4.7 du guide backend, "appel technique service-a-service").
 * <p>
 * Tant que ce client technique n'est pas configure (KEYCLOAK_ADMIN_CLIENT_ID absent), le
 * provisioning est ignore avec un simple avertissement : la creation de l'utilisateur cote GPDI
 * reste possible, il faut alors creer le compte Keycloak manuellement.
 */
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestTemplate restTemplate;

    @Value("${dsi.gpdi.keycloak.admin-base-url}")
    private String baseUrl;

    @Value("${dsi.gpdi.keycloak.realm}")
    private String realm;

    @Value("${dsi.gpdi.keycloak.admin-client-id}")
    private String adminClientId;

    @Value("${dsi.gpdi.keycloak.admin-client-secret}")
    private String adminClientSecret;

    /** Client applicatif portant les roles GESTIONNAIRE_* lus par KeycloakRoleConverter. */
    @Value("${dsi.gpdi.security.keycloak-client-id}")
    private String applicationClientId;

    private static final String PREFIXE_ROLES_GESTIONNAIRE = "GESTIONNAIRE_";

    public boolean estConfigure() {
        return StringUtils.hasText(adminClientId) && StringUtils.hasText(adminClientSecret);
    }

    /**
     * Cree le compte Keycloak d'un utilisateur GPDI. Le nom d'utilisateur Keycloak est toujours
     * le matricule (cf. UtilisateurCourantService, qui resout l'utilisateur courant par
     * preferred_username = matricule).
     * <p>
     * Tous les roles GESTIONNAIRE_* du client applicatif sont attribues au compte cree (choix
     * provisoire : le controle fin reste porte par la matrice d'habilitations GPDI). Si cette
     * attribution echoue, le compte Keycloak tout juste cree est supprime pour ne pas laisser de
     * compte orphelin, puis l'exception remonte (la transaction GPDI est alors annulee).
     * Necessite les roles realm-management "manage-users" et "view-clients" sur le client technique.
     */
    public void creerUtilisateur(String matricule, String motDePasseClair, String email, String prenom, String nom) {
        if (!estConfigure()) {
            log.warn("Provisioning Keycloak non configure (KEYCLOAK_ADMIN_CLIENT_ID absent) : "
                    + "le compte Keycloak du matricule {} devra etre cree manuellement.", matricule);
            return;
        }

        try {
            String jeton = obtenirJetonAdmin();

            Map<String, Object> identifiants = new HashMap<>();
            identifiants.put("type", "password");
            identifiants.put("value", motDePasseClair);
            identifiants.put("temporary", false);

            Map<String, Object> corps = new HashMap<>();
            corps.put("username", matricule);
            corps.put("enabled", true);
            corps.put("email", email);
            corps.put("firstName", prenom);
            corps.put("lastName", nom);
            corps.put("credentials", Collections.singletonList(identifiants));

            restTemplate.postForEntity(baseUrl + "/admin/realms/" + realm + "/users",
                    new HttpEntity<>(corps, enteteAvecJeton(jeton)), Void.class);
            log.info("Compte Keycloak cree pour le matricule {}", matricule);
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException(
                    "Le compte GPDI n'a pas pu être créé : le service d'authentification Keycloak a refusé ou n'a pas pu traiter la demande.", e);
        }

        attribuerRolesGestionnaire(matricule);
    }

    private void attribuerRolesGestionnaire(String matricule) {
        try {
            String jeton = obtenirJetonAdmin();
            String idKeycloak = rechercherIdParMatricule(matricule, jeton);
            String idClient = rechercherIdClient(applicationClientId, jeton);
            if (idKeycloak == null || idClient == null) {
                throw new KeycloakIndisponibleException("Compte Keycloak ou client " + applicationClientId + " introuvable.");
            }

            ResponseEntity<List<Map<String, Object>>> reponse = restTemplate.exchange(
                    baseUrl + "/admin/realms/" + realm + "/clients/" + idClient + "/roles",
                    HttpMethod.GET, new HttpEntity<>(enteteAvecJeton(jeton)),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() { });
            List<Map<String, Object>> rolesClient = reponse.getBody() == null ? Collections.emptyList() : reponse.getBody();
            List<Map<String, Object>> rolesGestionnaire = rolesClient.stream()
                    .filter(role -> String.valueOf(role.get("name")).startsWith(PREFIXE_ROLES_GESTIONNAIRE))
                    .collect(Collectors.toList());
            if (rolesGestionnaire.isEmpty()) {
                log.warn("Aucun role {}* trouve sur le client {} : aucun role attribue au matricule {}.",
                        PREFIXE_ROLES_GESTIONNAIRE, applicationClientId, matricule);
                return;
            }

            restTemplate.postForEntity(
                    baseUrl + "/admin/realms/" + realm + "/users/" + idKeycloak + "/role-mappings/clients/" + idClient,
                    new HttpEntity<>(rolesGestionnaire, enteteAvecJeton(jeton)), Void.class);
            log.info("{} role(s) GESTIONNAIRE attribue(s) au compte Keycloak du matricule {}", rolesGestionnaire.size(), matricule);
        } catch (RestClientException | KeycloakIndisponibleException e) {
            supprimerUtilisateur(matricule);
            throw new KeycloakIndisponibleException(
                    "Le compte GPDI n'a pas pu être créé : les rôles n'ont pas pu être attribués dans Keycloak "
                            + "(vérifier les droits manage-users et view-clients du client technique).", e);
        }
    }

    private String rechercherIdClient(String clientId, String jeton) {
        ResponseEntity<List<Map<String, Object>>> reponse = restTemplate.exchange(
                baseUrl + "/admin/realms/" + realm + "/clients?clientId=" + clientId,
                HttpMethod.GET, new HttpEntity<>(enteteAvecJeton(jeton)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() { });
        List<Map<String, Object>> resultats = reponse.getBody();
        if (resultats == null || resultats.isEmpty()) return null;
        return String.valueOf(resultats.get(0).get("id"));
    }

    /** Suppression best-effort : ne bloque jamais la suppression cote GPDI, journalise seulement en cas d'echec. */
    public void supprimerUtilisateur(String matricule) {
        if (!estConfigure()) return;
        try {
            String jeton = obtenirJetonAdmin();
            String idKeycloak = rechercherIdParMatricule(matricule, jeton);
            if (idKeycloak == null) {
                log.warn("Aucun compte Keycloak trouve pour le matricule {} (suppression ignoree)", matricule);
                return;
            }
            restTemplate.exchange(baseUrl + "/admin/realms/" + realm + "/users/" + idKeycloak,
                    HttpMethod.DELETE, new HttpEntity<>(enteteAvecJeton(jeton)), Void.class);
            log.info("Compte Keycloak supprime pour le matricule {}", matricule);
        } catch (RestClientException e) {
            log.warn("Echec de la suppression du compte Keycloak pour le matricule {} : {}", matricule, e.getMessage());
        }
    }

    /**
     * Recupere les derniers evenements de connexion journalises par Keycloak (LOGIN, LOGIN_ERROR,
     * LOGOUT...). Necessite que la journalisation des evenements soit activee sur le realm
     * (Realm settings > Events > User events settings > Save events). Renvoie une liste vide si
     * le provisioning n'est pas configure ou si l'appel echoue (le journal reste un outil de
     * consultation : son indisponibilite ne doit jamais faire echouer le reste de l'application).
     */
    public List<Map<String, Object>> obtenirEvenementsConnexion(int max) {
        if (!estConfigure()) {
            log.warn("Provisioning Keycloak non configure : journal des connexions indisponible.");
            return Collections.emptyList();
        }
        try {
            String jeton = obtenirJetonAdmin();
            String url = baseUrl + "/admin/realms/" + realm + "/events?max=" + max
                    + "&type=LOGIN&type=LOGIN_ERROR&type=LOGOUT&type=LOGOUT_ERROR";
            ResponseEntity<List<Map<String, Object>>> reponse = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(enteteAvecJeton(jeton)),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() { });
            List<Map<String, Object>> resultats = reponse.getBody();
            if (resultats == null) {
                return Collections.emptyList();
            }
            completerMatriculesManquants(resultats, jeton);
            return resultats;
        } catch (RestClientException e) {
            log.warn("Echec de la lecture du journal des connexions Keycloak : {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Les evenements LOGOUT de Keycloak ne portent pas details.username, seulement userId : le matricule
     * est alors recherche par identifiant Keycloak (une seule requete par utilisateur distinct), pour que
     * le journal puisse afficher le nom de l'agent. En cas d'echec, l'evenement reste inchange.
     */
    @SuppressWarnings("unchecked")
    private void completerMatriculesManquants(List<Map<String, Object>> evenements, String jeton) {
        Map<String, String> matriculesParId = new HashMap<>();
        for (Map<String, Object> evenement : evenements) {
            Map<String, Object> details = (Map<String, Object>) evenement.get("details");
            boolean matriculeConnu = details != null && details.get("username") != null;
            Object userId = evenement.get("userId");
            if (matriculeConnu || userId == null) {
                continue;
            }
            String idUtilisateur = String.valueOf(userId);
            if (!matriculesParId.containsKey(idUtilisateur)) {
                matriculesParId.put(idUtilisateur, rechercherMatriculeParId(idUtilisateur, jeton));
            }
            String matricule = matriculesParId.get(idUtilisateur);
            if (matricule != null) {
                Map<String, Object> detailsCompletes = details == null ? new HashMap<>() : new HashMap<>(details);
                detailsCompletes.put("username", matricule);
                evenement.put("details", detailsCompletes);
            }
        }
    }

    private String rechercherMatriculeParId(String idUtilisateur, String jeton) {
        try {
            ResponseEntity<Map<String, Object>> reponse = restTemplate.exchange(
                    baseUrl + "/admin/realms/" + realm + "/users/" + idUtilisateur,
                    HttpMethod.GET, new HttpEntity<>(enteteAvecJeton(jeton)),
                    new ParameterizedTypeReference<Map<String, Object>>() { });
            Map<String, Object> utilisateur = reponse.getBody();
            return utilisateur == null || utilisateur.get("username") == null ? null : String.valueOf(utilisateur.get("username"));
        } catch (RestClientException e) {
            log.warn("Matricule introuvable pour l'utilisateur Keycloak {} : {}", idUtilisateur, e.getMessage());
            return null;
        }
    }

    private String rechercherIdParMatricule(String matricule, String jeton) {
        ResponseEntity<List<Map<String, Object>>> reponse = restTemplate.exchange(
                baseUrl + "/admin/realms/" + realm + "/users?username=" + matricule + "&exact=true",
                HttpMethod.GET, new HttpEntity<>(enteteAvecJeton(jeton)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() { });
        List<Map<String, Object>> resultats = reponse.getBody();
        if (resultats == null || resultats.isEmpty()) return null;
        return String.valueOf(resultats.get(0).get("id"));
    }

    private String obtenirJetonAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> corps = new LinkedMultiValueMap<>();
        corps.add("grant_type", "client_credentials");
        corps.add("client_id", adminClientId);
        corps.add("client_secret", adminClientSecret);

        ResponseEntity<Map<String, Object>> reponse = restTemplate.exchange(
                baseUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                HttpMethod.POST, new HttpEntity<>(corps, headers),
                new ParameterizedTypeReference<Map<String, Object>>() { });

        Map<String, Object> corpsReponse = reponse.getBody();
        if (corpsReponse == null || corpsReponse.get("access_token") == null) {
            throw new KeycloakIndisponibleException("Jeton d'administration Keycloak non obtenu.");
        }
        return String.valueOf(corpsReponse.get("access_token"));
    }

    private HttpHeaders enteteAvecJeton(String jeton) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jeton);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
