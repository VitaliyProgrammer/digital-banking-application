package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.OutboxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Safe on several instances at once: SKIP LOCKED gives each relay its own batch. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.outbox.relay-enabled", havingValue = "true")
public class OutboxScheduler {

    private final OutboxRelay outboxRelay;

    private final OutboxProperties properties;

    /**
     * A full batch means more is probably waiting: take the next one at once instead of sleeping.
     * A smaller one (or a failure) means the backlog is gone, or the broker is down: wait for the next run.
     */
    @Scheduled(fixedDelayString = "${banking.outbox.relay-delay}")
    public void publishDue() {

        while (outboxRelay.publishDue() == properties.batchSize()) {
            // next batch
        }
    }
}
