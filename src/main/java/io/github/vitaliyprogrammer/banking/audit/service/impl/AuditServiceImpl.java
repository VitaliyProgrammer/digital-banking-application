package io.github.vitaliyprogrammer.banking.audit.service.impl;

import io.github.vitaliyprogrammer.banking.audit.dto.request.AuditSearchRequestDto;
import io.github.vitaliyprogrammer.banking.audit.dto.response.AuditEntryResponseDto;
import io.github.vitaliyprogrammer.banking.audit.entity.AuditEntry;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditTarget;
import io.github.vitaliyprogrammer.banking.audit.repository.AuditEntryRepository;
import io.github.vitaliyprogrammer.banking.audit.service.AuditService;
import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditEntryRepository auditEntryRepository;

    private final UserService userService;

    private final JsonMapper jsonMapper;

    private final Clock clock;

    /** MANDATORY for the same reason as the outbox: an entry in a transaction of its own would outlive a rollback. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, AuditAction action, AuditTarget targetType, UUID targetId,
                       Map<String, ?> details) {

        auditEntryRepository.save(AuditEntry.of(actorId, userService.getById(actorId).role(), action, targetType,
                targetId, jsonMapper.writeValueAsString(details), clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<AuditEntryResponseDto> search(AuditSearchRequestDto filter, int page, int size) {

        return PageResponseDto.from(
                auditEntryRepository.findAll(matching(filter),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"))),
                this::toResponse);
    }

    /** Only the filters that were given become conditions; none at all means "everything". */
    private static Specification<AuditEntry> matching(AuditSearchRequestDto filter) {

        return (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();

            if (filter.actorId() != null) {
                conditions.add(builder.equal(root.get("actorId"), filter.actorId()));
            }
            if (filter.action() != null) {
                conditions.add(builder.equal(root.get("action"), filter.action()));
            }
            if (filter.targetType() != null) {
                conditions.add(builder.equal(root.get("targetType"), filter.targetType()));
            }
            if (filter.targetId() != null) {
                conditions.add(builder.equal(root.get("targetId"), filter.targetId()));
            }
            if (filter.from() != null) {
                conditions.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
            }
            if (filter.to() != null) {
                conditions.add(builder.lessThan(root.get("occurredAt"), filter.to()));
            }

            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }

    private AuditEntryResponseDto toResponse(AuditEntry entry) {

        return new AuditEntryResponseDto(entry.getId(), entry.getActorId(), entry.getActorRole(), entry.getAction(),
                entry.getTargetType(), entry.getTargetId(), jsonMapper.readTree(entry.getDetails()),
                entry.getOccurredAt());
    }
}
