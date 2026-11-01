package io.github.vitaliyprogrammer.banking.identity.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from "banking.security.jwt.*". Validated on startup: the app refuses to start
 * with a missing or too short secret instead of failing on the first login.
 */
@Validated
@ConfigurationProperties(prefix = "banking.security.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32, message = "must be at least 32 characters (256 bits) for HS256") String secret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl
) {
}
