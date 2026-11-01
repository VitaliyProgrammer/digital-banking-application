package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.entity.status.AuditTarget;
import io.github.vitaliyprogrammer.banking.entity.status.Role;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record AuditEntryResponseDto(
        UUID id,
        UUID actorId,
        Role actorRole,
        AuditAction action,
        AuditTarget targetType,
        UUID targetId,
        /* Differs per action: from which state to which, the amounts, the comment. */
        JsonNode details,
        Instant occurredAt
) {
}
