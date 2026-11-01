package io.github.vitaliyprogrammer.banking.credit.event;

import io.github.vitaliyprogrammer.banking.credit.entity.CreditApplication;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.credit.entity.status.DecisionSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The body of credit.application.decided. */
public record CreditDecisionMessage(
        UUID applicationId,
        UUID userId,
        CreditApplicationStatus status,
        DecisionSource decisionSource,
        BigDecimal requestedLimit,
        BigDecimal approvedLimit,
        Instant decidedAt
) {

    public static CreditDecisionMessage from(CreditApplication application) {
        return new CreditDecisionMessage(
                application.getId(),
                application.getUserId(),
                application.getStatus(),
                application.getDecisionSource(),
                application.getRequestedLimit(),
                application.getApprovedLimit(),
                application.getDecidedAt());
    }
}
