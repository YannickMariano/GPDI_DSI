package mg.gov.mef.dsi.gpdi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mg.gov.mef.dsi.gpdi.exception.ErreurResponse;
import mg.gov.mef.dsi.gpdi.security.KeycloakRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakRoleConverter keycloakRoleConverter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .antMatchers("/api/utilisateurs/moi").authenticated()
                .antMatchers("/api/notifications/**").authenticated()
                .antMatchers("/api/journal-connexions").hasRole("GESTIONNAIRE_UTILISATEUR")
                .antMatchers("/api/roles/**").hasRole("GESTIONNAIRE_ROLE")
                .antMatchers("/api/habilitations/**").hasRole("GESTIONNAIRE_ROLE")
                .antMatchers("/api/utilisateurs/**").hasRole("GESTIONNAIRE_UTILISATEUR")
                .antMatchers("/api/equipes/**").hasRole("GESTIONNAIRE_EQUIPE")
                .antMatchers("/api/projets/*/sprints/**").hasRole("GESTIONNAIRE_SPRINT")
                .antMatchers("/api/projets/*/jalons/**").hasRole("GESTIONNAIRE_JALON")
                .antMatchers("/api/jalons/*/livrables/**").hasRole("GESTIONNAIRE_LIVRABLE")
                .antMatchers("/api/projets/*/risques/**").hasRole("GESTIONNAIRE_RISQUE")
                .antMatchers("/api/projets/*/taches/**").hasRole("GESTIONNAIRE_TACHE")
                .antMatchers("/api/taches/**").hasRole("GESTIONNAIRE_TACHE")
                // Pas de role dedie : le perimetre (chef de projet du projet, ou compte client avec un
                // partage actif sur ce projet precis) est verifie dans PointInformationServiceImpl.
                .antMatchers("/api/projets/*/points-information/**").authenticated()
                // Meme role que le reste de la gestion de projet ; le perimetre (chef de projet du
                // projet precis) est en plus verifie dans AssistantIaServiceImpl.
                .antMatchers("/api/projets/*/assistant/**").hasRole("GESTIONNAIRE_PROJET")
                // Lecture seule ouverte aussi a GESTIONNAIRE_PARTAGE (Directeur DSI, vue portefeuille) ;
                // generer/appliquer/rejeter restent reserves a GESTIONNAIRE_PROJET, via le matcher suivant.
                // Le perimetre exact (chef de projet, ou Directeur DSI en lecture) est verifie dans
                // RecommandationProjetServiceImpl.
                .antMatchers(HttpMethod.GET, "/api/projets/*/recommandations").hasAnyRole("GESTIONNAIRE_PROJET", "GESTIONNAIRE_PARTAGE")
                .antMatchers("/api/projets/*/recommandations/**").hasRole("GESTIONNAIRE_PROJET")
                // Lecture seule ouverte a GESTIONNAIRE_PARTAGE : il faut pouvoir lister les projets
                // pour choisir lesquels partager avec un compte client, sans droit de gestion complet.
                .antMatchers(HttpMethod.GET, "/api/projets").hasAnyRole("GESTIONNAIRE_PROJET", "GESTIONNAIRE_PARTAGE")
                .antMatchers("/api/projets/**").hasRole("GESTIONNAIRE_PROJET")
                .antMatchers("/api/partages-clients/**").hasRole("GESTIONNAIRE_PARTAGE")
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .accessDeniedHandler((request, response, ex) -> ecrireErreur(response, HttpServletResponse.SC_FORBIDDEN,
                        "Accès refusé : votre compte ne dispose pas des droits nécessaires pour cette action."))
                .authenticationEntryPoint((request, response, ex) -> ecrireErreur(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authentification requise : jeton absent, invalide ou expiré."))
                .and()
                .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(keycloakRoleConverter);
        return http.build();
    }

    private void ecrireErreur(HttpServletResponse response, int statut, String message) throws java.io.IOException {
        response.setStatus(statut);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ErreurResponse(message)));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
