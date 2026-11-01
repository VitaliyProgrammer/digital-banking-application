package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.OutboxProperties;
import io.github.vitaliyprogrammer.banking.entity.OutboxEvent;
import io.github.vitaliyprogrammer.banking.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Moves committed events from the outbox table to RabbitMQ. Delivery is AT LEAST ONCE, never "at most once":
 * <ul>
 *   <li>the broker confirms every message before the row is marked published;</li>
 *   <li>if the process dies after the confirm but before the commit, the event is published again next time.</li>
 * </ul>
 * So a consumer may see a message twice and must deduplicate by the message id (the outbox row id).
 * Ordering is by creation time within one batch, but not guaranteed across relay instances: consumers must not
 * depend on it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;

    private final RabbitTemplate rabbitTemplate;

    private final TransactionTemplate transactionTemplate;

    private final OutboxProperties properties;

    private final Clock clock;

    /**
     * One batch in one transaction: the rows stay locked while they are published, so a second relay skips them.
     * The first failure ends the batch: the broker is most likely down, and the rest would only fail slower.
     * Returns how many events were published.
     */
    public int publishDue() {

        Integer published = transactionTemplate.execute(_ -> {
            Instant now = clock.instant();
            int count = 0;

            for (OutboxEvent event : outboxEventRepository.lockDue(now, properties.batchSize())) {
                try {
                    publish(event);
                    event.markPublished(clock.instant());
                    count++;
                } catch (AmqpException e) {
                    event.markFailed(e.getMessage(),
                            // From the failure, not from the start of the batch: a slow broker must not eat up the wait.
                            clock.instant().plus(OutboxBackoff.delayAfter(event.getAttempts() + 1, properties.maxBackoff())));
                    log.warn("Outbox event {} ({}) not published, attempt {}: {}",
                            event.getId(), event.getEventType(), event.getAttempts(), e.getMessage());
                    break;
                }
            }

            return count;
        });

        return Objects.requireNonNull(published);
    }

    /** Persistent message + publisher confirm: once the broker says "ack", the message survives its restart. */
    private void publish(OutboxEvent event) {

        Message message = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.getId().toString())
                .setType(event.getEventType())
                .setTimestamp(Date.from(event.getCreatedAt()))
                .setHeader("aggregate-id", event.getAggregateId().toString())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        rabbitTemplate.invoke(operations -> {
            operations.send(properties.exchange(), event.getEventType(), message);
            operations.waitForConfirmsOrDie(properties.confirmTimeout().toMillis());
            return null;
        });
    }
}
