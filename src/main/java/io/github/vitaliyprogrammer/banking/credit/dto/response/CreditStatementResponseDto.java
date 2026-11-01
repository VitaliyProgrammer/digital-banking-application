package io.github.vitaliyprogrammer.banking.credit.dto.response;

import io.github.vitaliyprogrammer.banking.credit.entity.CreditStatement;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditStatementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One month of the credit line; repaid, interest and lateFee are null until the due date has passed. */
public record CreditStatementResponseDto(
        String period,
        BigDecimal closingDebt,
        BigDecimal minimumPayment,
        LocalDate dueDate,
        CreditStatementStatus status,
        BigDecimal repaid,
        BigDecimal interest,
        BigDecimal lateFee,
        Instant settledAt
) {

    public static CreditStatementResponseDto from(CreditStatement statement) {
        return new CreditStatementResponseDto(
                statement.getPeriod(),
                statement.getClosingDebt(),
                statement.getMinimumPayment(),
                statement.getDueDate(),
                statement.getStatus(),
                statement.getRepaid(),
                statement.getInterest(),
                statement.getLateFee(),
                statement.getSettledAt());
    }
}
