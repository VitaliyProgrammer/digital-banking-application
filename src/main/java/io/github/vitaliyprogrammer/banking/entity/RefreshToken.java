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
import org.hibernate.annotations.UuidGenerator;

/**
 * One login session. Only the SHA-256 hash of the token is stored: a leaked table cannot be used to log in.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID userId;

    private String tokenHash;

    private Instant expiresAt;

    private Instant revokedAt;

    private Instant createdAt;

    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt, Instant now) {
        RefreshToken token = new RefreshToken();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.createdAt = now;
        return token;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
