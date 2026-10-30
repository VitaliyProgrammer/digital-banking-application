package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.SupportTicket;
import io.github.vitaliyprogrammer.banking.entity.status.TicketStatus;
import java.time.Instant;
import java.util.UUID;

/** A ticket without its conversation: for lists and the queue. */
public record TicketResponseDto(
        UUID id,
        UUID clientId,
        String subject,
        TicketStatus status,
        /* The employee who took it; null while in the queue. */
        UUID assigneeId,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {

    public static TicketResponseDto from(SupportTicket ticket) {
        return new TicketResponseDto(
                ticket.getId(),
                ticket.getClientId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt());
    }
}
