package io.github.vitaliyprogrammer.banking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** One event waiting to be published, or already published. Its id travels as the message id. */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private String eventType;

    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    private Instant createdAt;

    private Instant publishedAt;

    private int attempts;

    private Instant nextAttemptAt;

    private String lastError;

    public static OutboxEvent create(String eventType, UUID aggregateId, String payload, Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.eventType = eventType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.createdAt = now;
        event.nextAttemptAt = now;
        return event;
    }

    public void markPublished(Instant now) {
        this.publishedAt = now;
        this.attempts++;
        this.lastError = null;
    }

    public void markFailed(String error, Instant nextAttemptAt) {
        this.attempts++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
