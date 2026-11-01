package io.github.vitaliyprogrammer.banking.notification.service.impl;

import io.github.vitaliyprogrammer.banking.notification.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * The inbound adapter: a message in, a notification out. Two kinds of failure, two different fates:
 * <ul>
 *   <li>a message that can never succeed (unknown type, broken JSON, no id) goes straight to the dead-letter queue;</li>
 *   <li>anything else (the database is down) is retried a few times, then dead-lettered too.</li>
 * </ul>
 * Duplicates need no handling here: the service is idempotent by the message id.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    public static final String LISTENER_ID = "notifications";

    private final NotificationService notificationService;

    @RabbitListener(id = LISTENER_ID, queues = "${banking.notifications.queue}",
            autoStartup = "${banking.notifications.listener-enabled}")
    public void onEvent(Message message) {

        String messageId = message.getMessageProperties().getMessageId();
        String eventType = message.getMessageProperties().getReceivedRoutingKey();

        try {
            boolean created = notificationService.record(
                    UUID.fromString(messageId), eventType, new String(message.getBody(), StandardCharsets.UTF_8));

            if (!created) {
                log.debug("Event {} ({}) was already turned into a notification", messageId, eventType);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AmqpRejectAndDontRequeueException("Unprocessable event " + messageId + " (" + eventType + ")", e);
        }
    }
}
