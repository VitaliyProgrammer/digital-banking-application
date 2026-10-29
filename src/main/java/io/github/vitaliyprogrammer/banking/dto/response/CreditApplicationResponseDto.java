package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.CreditApplication;
import io.github.vitaliyprogrammer.banking.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.entity.status.DecisionSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A credit application as both the client and the reviewer see it, with the reasons behind the decision. */
public record CreditApplicationResponseDto(
        UUID id,
        UUID userId,
        BigDecimal requestedLimit,
        BigDecimal affordableLimit,
        BigDecimal approvedLimit,
        CreditApplicationStatus status,
        DecisionSource decisionSource,
        List<String> reasons,
        String decisionComment,
        Instant createdAt,
        Instant decidedAt
) {

    public static CreditApplicationResponseDto from(CreditApplication application) {
        return new CreditApplicationResponseDto(
                application.getId(),
                application.getUserId(),
                application.getRequestedLimit(),
                application.getAffordableLimit(),
                application.getApprovedLimit(),
                application.getStatus(),
                application.getDecisionSource(),
                application.reasonList(),
                application.getDecisionComment(),
                application.getCreatedAt(),
                application.getDecidedAt());
    }
}
