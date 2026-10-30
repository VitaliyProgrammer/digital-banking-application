package io.github.vitaliyprogrammer.banking.event;

import io.github.vitaliyprogrammer.banking.entity.CreditStatement;
import io.github.vitaliyprogrammer.banking.entity.status.CreditStatementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The body of credit.statement.settled. */
public record CreditStatementMessage(
        UUID statementId,
        UUID userId,
        String period,
        CreditStatementStatus status,
        BigDecimal closingDebt,
        BigDecimal repaid,
        BigDecimal interest,
        BigDecimal lateFee,
        LocalDate dueDate,
        Instant settledAt
) {

    public static CreditStatementMessage from(CreditStatement statement, UUID userId) {
        return new CreditStatementMessage(
                statement.getId(),
                userId,
                statement.getPeriod(),
                statement.getStatus(),
                statement.getClosingDebt(),
                statement.getRepaid(),
                statement.getInterest(),
                statement.getLateFee(),
                statement.getDueDate(),
                statement.getSettledAt());
    }
}
