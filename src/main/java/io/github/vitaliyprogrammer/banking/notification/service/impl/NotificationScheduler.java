package io.github.vitaliyprogrammer.banking.notification.service.impl;

import io.github.vitaliyprogrammer.banking.notification.configuration.NotificationProperties;
import io.github.vitaliyprogrammer.banking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Safe on several instances at once: SKIP LOCKED gives each its own batch of emails. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.notifications.mailer-enabled", havingValue = "true")
public class NotificationScheduler {

    private final NotificationService notificationService;

    private final NotificationProperties properties;

    /** A full batch means more is probably waiting: take the next one at once. */
    @Scheduled(fixedDelayString = "${banking.notifications.mailer-delay}")
    public void sendDueEmails() {

        while (notificationService.sendDueEmails() == properties.mailerBatchSize()) {
            // next batch
        }
    }
}
