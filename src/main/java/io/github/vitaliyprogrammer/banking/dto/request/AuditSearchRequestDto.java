package io.github.vitaliyprogrammer.banking.dto.request;

import io.github.vitaliyprogrammer.banking.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.entity.status.AuditTarget;
import java.time.Instant;
import java.util.UUID;

/** Every filter is optional; the ones given are combined with AND. */
public record AuditSearchRequestDto(
        /* Who did it: "everything this employee did". */
        UUID actorId,
        AuditAction action,
        /* What it was about: "everything that happened to this application". */
        AuditTarget targetType,
        UUID targetId,
        Instant from,
        Instant to
) {
}
