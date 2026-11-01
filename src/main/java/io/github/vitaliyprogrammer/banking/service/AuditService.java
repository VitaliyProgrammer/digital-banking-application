package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.AuditSearchRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AuditEntryResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PageResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.entity.status.AuditTarget;
import java.util.Map;
import java.util.UUID;

public interface AuditService {

    /**
     * Writes an entry into the caller's transaction: it exists only if the audited action commits.
     * Calling it outside of a transaction is a bug and fails at once.
     */
    void record(UUID actorId, AuditAction action, AuditTarget targetType, UUID targetId, Map<String, ?> details);

    /** For ADMIN: newest first. */
    PageResponseDto<AuditEntryResponseDto> search(AuditSearchRequestDto filter, int page, int size);
}
