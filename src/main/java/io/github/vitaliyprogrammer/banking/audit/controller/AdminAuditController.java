package io.github.vitaliyprogrammer.banking.audit.controller;

import io.github.vitaliyprogrammer.banking.audit.dto.request.AuditSearchRequestDto;
import io.github.vitaliyprogrammer.banking.audit.dto.response.AuditEntryResponseDto;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.audit.entity.status.AuditTarget;
import io.github.vitaliyprogrammer.banking.audit.service.AuditService;
import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN only (under /admin/**). Support does not read the log that records support's own actions:
 * separation of duties.
 */
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Who did what, when: every action of bank employees")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Search the audit log",
            description = "Newest first. Every filter is optional: by employee, by action, by what it was about, "
                    + "by time (from inclusive, to exclusive, ISO-8601)")
    public PageResponseDto<AuditEntryResponseDto> search(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditTarget targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {

        return auditService.search(new AuditSearchRequestDto(actorId, action, targetType, targetId, from, to),
                page, size);
    }
}
