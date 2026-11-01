package io.github.vitaliyprogrammer.banking.support.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** One line of a support conversation. Append-only: the database refuses to change or delete it. */
@Entity
@Table(name = "ticket_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketMessage {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID ticketId;

    private UUID authorId;

    private boolean fromStaff;

    private String body;

    private Instant createdAt;

    public static TicketMessage of(UUID ticketId, UUID authorId, boolean fromStaff, String body, Instant now) {
        TicketMessage message = new TicketMessage();
        message.ticketId = ticketId;
        message.authorId = authorId;
        message.fromStaff = fromStaff;
        message.body = body;
        message.createdAt = now;
        return message;
    }
}
