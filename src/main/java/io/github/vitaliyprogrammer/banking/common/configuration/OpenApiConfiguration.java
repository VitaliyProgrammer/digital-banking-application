package io.github.vitaliyprogrammer.banking.common.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html. Endpoints marked with the "bearerAuth" requirement
 * get an "Authorize" button for pasting the access token.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "Digital Banking API", version = "v1"))
@SecurityScheme(name = OpenApiConfiguration.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";
}
