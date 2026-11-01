package io.github.vitaliyprogrammer.banking.notification.service;

import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.notification.dto.response.NotificationResponseDto;
import java.util.UUID;

public interface NotificationService {

    /**
     * For the consumer: turns one event into a notification. Idempotent by the event id: the same event delivered
     * again changes nothing and returns false. An unknown event type or a broken payload is an
     * IllegalArgumentException, which the consumer sends to the dead-letter queue.
     */
    boolean record(UUID eventId, String eventType, String payload);

    /** For the email job: sends the due email copies; returns how many were sent. */
    int sendDueEmails();

    PageResponseDto<NotificationResponseDto> getNotifications(UUID userId, int page, int size);

    long getUnreadCount(UUID userId);

    NotificationResponseDto markRead(UUID userId, UUID notificationId);

    int markAllRead(UUID userId);
}
