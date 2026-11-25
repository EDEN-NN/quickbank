package com.quickbank.bootstrap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI quickBankOpenAPI(@Value("${quickbank.security.issuer}") String issuer) {
        return new OpenAPI()
                .info(new Info()
                        .title("QuickBank API")
                        .version("0.1.0")
                        .description("Account, deposit, transfer, and statement calls stay behind a Keycloak JWT with the CLIENT realm role. POST /api/auth/login, /api/auth/registrations, and /api/auth/refresh are public."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Keycloak access token. Paste the token only; Swagger sends Authorization: Bearer. "
                                + "Issuer " + issuer + ". The token must include the CLIENT realm role.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
