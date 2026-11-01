package io.github.vitaliyprogrammer.banking.support.entity;

import io.github.vitaliyprogrammer.banking.support.entity.status.TicketStatus;
import io.github.vitaliyprogrammer.banking.support.exception.InvalidTicketStateException;
import io.github.vitaliyprogrammer.banking.support.exception.NotTicketAssigneeException;
import io.github.vitaliyprogrammer.banking.support.exception.OwnTicketException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * The state machine of a support request. Every transition checks who is asking and in which state the ticket is;
 * the caller holds a row lock, so two employees can never both take or both resolve the same ticket.
 */
@Entity
@Table(name = "support_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportTicket {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID clientId;

    private String subject;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private UUID assigneeId;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant resolvedAt;

    public static SupportTicket open(UUID clientId, String subject, Instant now) {
        SupportTicket ticket = new SupportTicket();
        ticket.clientId = clientId;
        ticket.subject = subject;
        ticket.status = TicketStatus.OPEN;
        ticket.createdAt = now;
        ticket.updatedAt = now;
        return ticket;
    }

    public void take(UUID staffId, Instant now) {
        if (clientId.equals(staffId)) {
            throw new OwnTicketException();
        }
        requireStatus(TicketStatus.OPEN, "already taken");
        assigneeId = staffId;
        status = TicketStatus.IN_PROGRESS;
        updatedAt = now;
    }

    /** Only the employee who took the ticket answers it. */
    public void recordStaffReply(UUID staffId, Instant now) {
        requireAssignee(staffId);
        requireStatus(TicketStatus.IN_PROGRESS, "not in progress");
        updatedAt = now;
    }

    public void recordClientReply(Instant now) {
        if (status == TicketStatus.RESOLVED) {
            throw new InvalidTicketStateException("The ticket is resolved; open a new one");
        }
        updatedAt = now;
    }

    public void resolve(UUID staffId, Instant now) {
        requireAssignee(staffId);
        requireStatus(TicketStatus.IN_PROGRESS, "not in progress");
        status = TicketStatus.RESOLVED;
        resolvedAt = now;
        updatedAt = now;
    }

    public void requireAssignee(UUID staffId) {
        if (!staffId.equals(assigneeId)) {
            throw new NotTicketAssigneeException();
        }
    }

    private void requireStatus(TicketStatus expected, String problem) {
        if (status != expected) {
            throw new InvalidTicketStateException("The ticket is " + problem + ": " + status);
        }
    }
}
