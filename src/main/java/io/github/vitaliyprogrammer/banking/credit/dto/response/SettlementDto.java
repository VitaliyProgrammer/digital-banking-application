package io.github.vitaliyprogrammer.banking.credit.dto.response;

import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditStatementStatus;
import java.math.BigDecimal;

/** Internal: what a statement costs the client once its due date has passed. */
public record SettlementDto(
        CreditStatementStatus status,
        BigDecimal interest,
        BigDecimal lateFee
) {

    public BigDecimal charges() {
        return interest.add(lateFee);
    }
}
