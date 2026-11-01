package io.github.vitaliyprogrammer.banking.credit.dto.response;

import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditApplicationStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Internal: what the scoring rules decided. {@code approvedLimit} is set only for APPROVED;
 * {@code reasons} explain the decision in words a reviewer and the client can read.
 */
public record ScoringResultDto(
        CreditApplicationStatus status,
        BigDecimal affordableLimit,
        BigDecimal approvedLimit,
        List<String> reasons
) {
}
