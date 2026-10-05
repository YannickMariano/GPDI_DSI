package mg.gov.mef.dsi.gpdi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final String clientId;

    public KeycloakRoleConverter(@Value("${dsi.gpdi.security.keycloak-client-id}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> autorites = extraireRoles(jwt).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        return new JwtAuthenticationToken(jwt, autorites);
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extraireRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return Collections.emptyList();
        }
        Map<String, Object> client = (Map<String, Object>) resourceAccess.get(clientId);
        if (client == null) {
            return Collections.emptyList();
        }
        return (Collection<String>) client.getOrDefault("roles", Collections.emptyList());
    }
}
