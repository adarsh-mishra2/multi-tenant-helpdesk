package com.assignment.helpdesk.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI helpdeskOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Multi-Tenant Helpdesk API")
                        .version("v1")
                        .description("""
                                Ticketing API where each organization (tenant) has its own users and tickets.
                                Roles: ORG_ADMIN, AGENT, CUSTOMER. All data is scoped to the caller's organization,
                                which is taken from the authenticated user, never from the request.
                                Errors follow RFC 7807 (application/problem+json)."""))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .name(BEARER_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
