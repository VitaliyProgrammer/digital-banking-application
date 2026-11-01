package io.github.vitaliyprogrammer.banking.audit.entity;

import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditTarget;
import io.github.vitaliyprogrammer.banking.identity.entity.status.Role;
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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** One line of the audit log. Immutable for Hibernate too: it never even tries to update it. */
@Entity
@Immutable
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEntry {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID actorId;

    @Enumerated(EnumType.STRING)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    private AuditTarget targetType;

    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String details;

    private Instant occurredAt;

    public static AuditEntry of(UUID actorId, Role actorRole, AuditAction action, AuditTarget targetType,
                                UUID targetId, String details, Instant now) {
        AuditEntry entry = new AuditEntry();
        entry.actorId = actorId;
        entry.actorRole = actorRole;
        entry.action = action;
        entry.targetType = targetType;
        entry.targetId = targetId;
        entry.details = details;
        entry.occurredAt = now;
        return entry;
    }
}
