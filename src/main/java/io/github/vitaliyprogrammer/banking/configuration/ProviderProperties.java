package io.github.vitaliyprogrammer.banking.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.providers.*". */
@Validated
@ConfigurationProperties(prefix = "banking.providers")
public record ProviderProperties(
        @NotBlank
        String baseUrl,

        /* How long to wait for the connection to open. Failing here means the request was never sent. */
        @NotNull
        Duration connectTimeout,

        /* How long to wait for the answer. Failing here means we do NOT know what the provider did. */
        @NotNull
        Duration readTimeout,

        /* Built-in fake providers at /mock-providers for local runs and demos. Off in real environments. */
        boolean mockEnabled
) {
}
