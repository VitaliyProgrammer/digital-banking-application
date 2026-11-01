package io.github.vitaliyprogrammer.banking.notification.dto.response;

/** The number on the bell. An object, not a bare number, so fields can be added without breaking clients. */
public record UnreadCountResponseDto(
        long unread
) {
}
