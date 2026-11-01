package io.github.vitaliyprogrammer.banking.support.dto.response;

import io.github.vitaliyprogrammer.banking.support.entity.TicketMessage;
import java.time.Instant;
import java.util.UUID;

/** Which employee wrote it is not shown: to the client the answer comes from "support", not from a person's id. */
public record TicketMessageResponseDto(
        UUID id,
        boolean fromStaff,
        String body,
        Instant createdAt
) {

    public static TicketMessageResponseDto from(TicketMessage message) {
        return new TicketMessageResponseDto(message.getId(), message.isFromStaff(), message.getBody(),
                message.getCreatedAt());
    }
}
