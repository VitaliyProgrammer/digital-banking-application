package io.github.vitaliyprogrammer.banking.notification.dto.response;

import io.github.vitaliyprogrammer.banking.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponseDto(
        UUID id,
        /* e.g. "payment.completed": lets the app pick an icon. */
        String eventType,
        String title,
        String body,
        Instant createdAt,
        /* Null while unread. */
        Instant readAt
) {

    public static NotificationResponseDto from(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
