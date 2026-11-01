package io.github.vitaliyprogrammer.banking.support.event;

import io.github.vitaliyprogrammer.banking.support.entity.SupportTicket;
import io.github.vitaliyprogrammer.banking.support.entity.status.TicketStatus;
import java.time.Instant;
import java.util.UUID;

/** The body of support.ticket.replied and support.ticket.resolved. The text of the answer stays in the bank. */
public record SupportTicketMessage(
        UUID ticketId,
        UUID userId,
        String subject,
        TicketStatus status,
        Instant occurredAt
) {

    public static SupportTicketMessage from(SupportTicket ticket) {
        return new SupportTicketMessage(ticket.getId(), ticket.getClientId(), ticket.getSubject(), ticket.getStatus(),
                ticket.getUpdatedAt());
    }
}
