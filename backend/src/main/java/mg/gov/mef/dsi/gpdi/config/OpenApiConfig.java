package mg.gov.mef.dsi.gpdi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

@OpenAPIDefinition(security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI gpdiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GPDI API")
                        .description("API du backend GPDI - DSI MEF")
                        .version("v1"));
    }
}
