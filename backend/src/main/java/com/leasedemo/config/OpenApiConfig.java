package com.leasedemo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI documentation configuration (M4.1.1).
 *
 * <p>Exposes:
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui/index.html}</li>
 *   <li>OpenAPI JSON: {@code /v3/api-docs}</li>
 * </ul>
 *
 * <p>Registers a manual HTTP Bearer JWT security scheme ({@code bearerAuth})
 * so a developer can paste a Keycloak-issued access token into Swagger's
 * "Authorize" dialog and exercise protected endpoints. Swagger UI itself
 * sends the standard {@code Authorization: Bearer <token>} header — Angular
 * remains the only component responsible for the real browser Keycloak
 * Authorization Code + PKCE login flow; this scheme is a developer/testing
 * convenience only, not a second authentication mechanism.
 *
 * <p><strong>Production note:</strong> for this demo milestone Swagger UI
 * and the OpenAPI document are publicly readable (see {@code SecurityConfig}).
 * In a real financial production environment, whether to expose interactive
 * API documentation publicly — versus restricting it to internal networks or
 * disabling it entirely — is an explicit deployment/security decision made
 * per environment, independent of this demo's configuration.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI leaseDemoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LeaseDemo API")
                        .description("REST API for the LeaseDemo leasing application.")
                        .version("0.1.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
