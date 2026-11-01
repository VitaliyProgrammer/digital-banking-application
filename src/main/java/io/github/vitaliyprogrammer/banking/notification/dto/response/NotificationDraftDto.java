package io.github.vitaliyprogrammer.banking.notification.dto.response;

import java.util.UUID;

/** Internal: what to tell whom about an event, and whether it is worth an email too. */
public record NotificationDraftDto(
        UUID userId,
        String title,
        String body,
        boolean emailRequired
) {
}
