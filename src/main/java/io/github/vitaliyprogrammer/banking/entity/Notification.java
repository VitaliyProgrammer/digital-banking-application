package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.EmailStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Inserted natively (ON CONFLICT DO NOTHING on the source event); read and changed through JPA. */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    private UUID id;

    private UUID userId;

    private UUID sourceEventId;

    private String eventType;

    private String title;

    private String body;

    private Instant createdAt;

    private Instant readAt;

    @Enumerated(EnumType.STRING)
    private EmailStatus emailStatus;

    private int emailAttempts;

    private Instant nextEmailAt;

    private Instant emailSentAt;

    private String emailError;

    /** Reading twice keeps the first time: "read at" is when the client first saw it. */
    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public void emailSent(Instant now) {
        emailStatus = EmailStatus.SENT;
        emailAttempts++;
        emailSentAt = now;
        nextEmailAt = null;
        emailError = null;
    }

    /** Either try again at {@code retryAt}, or give up when there is none. */
    public void emailFailed(String error, Instant retryAt) {
        emailAttempts++;
        emailError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
        nextEmailAt = retryAt;
        emailStatus = retryAt != null ? EmailStatus.PENDING : EmailStatus.FAILED;
    }
}
