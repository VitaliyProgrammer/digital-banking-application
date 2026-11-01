package io.github.vitaliyprogrammer.banking.outbox.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.outbox.*". */
@Validated
@ConfigurationProperties(prefix = "banking.outbox")
public record OutboxProperties(
        /* Off in tests, which run the relay themselves at the moment they choose. */
        boolean relayEnabled,

        @NotNull
        Duration relayDelay,

        /* Events per run, all in one short transaction. */
        @Min(1)
        int batchSize,

        /* How long to wait for the broker to confirm it has the message. */
        @NotNull
        Duration confirmTimeout,

        /* The longest wait between two attempts while the broker is down. */
        @NotNull
        Duration maxBackoff,

        /* The topic exchange every event is published to. */
        @NotBlank
        String exchange
) {
}
