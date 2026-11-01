package io.github.vitaliyprogrammer.banking.notification.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.notifications.*". */
@Validated
@ConfigurationProperties(prefix = "banking.notifications")
public record NotificationProperties(
        @NotBlank
        String queue,

        /* Messages that failed every attempt end up here, for a person to look at. */
        @NotBlank
        String deadLetterQueue,

        /* Off in tests, which start the listener only when they need it. */
        boolean listenerEnabled,

        /* Off in tests, which send due emails themselves. */
        boolean mailerEnabled,

        @NotNull
        Duration mailerDelay,

        @Min(1)
        int mailerBatchSize,

        /* After this many failed attempts an email is given up (the notification stays in the app). */
        @Min(1)
        int maxEmailAttempts,

        @NotNull
        Duration maxEmailBackoff,

        @NotBlank
        String mailFrom
) {
}
