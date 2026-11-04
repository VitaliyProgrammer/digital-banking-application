package io.github.vitaliyprogrammer.banking.identity.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.password-reset.*". */
@Validated
@ConfigurationProperties(prefix = "banking.password-reset")
public record PasswordResetProperties(
        /* How long a link in the email works. */
        @NotNull
        Duration tokenTtl,

        /* The page the link opens; the token is added as ?token=... */
        @NotBlank
        String linkBase,

        @NotBlank
        String mailFrom,

        /* At most this many emails per address in the window: nobody can flood someone else's inbox. */
        @Min(1)
        int maxRequests,

        @NotNull
        Duration requestWindow
) {
}
