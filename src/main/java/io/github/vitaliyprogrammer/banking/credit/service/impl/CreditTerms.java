package io.github.vitaliyprogrammer.banking.credit.service.impl;

import io.github.vitaliyprogrammer.banking.credit.configuration.CreditLineProperties;
import io.github.vitaliyprogrammer.banking.credit.dto.response.SettlementDto;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditStatementStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The price rules of a credit line, as pure functions of the numbers: no database, no clock.
 * Interest is charged only on what is still unpaid of a statement after its due date, never on money already
 * repaid, and a late fee only when even the minimum payment was missed.
 */
@Component
@RequiredArgsConstructor
public class CreditTerms {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CreditLineProperties properties;

    /** A share of the debt, at least the floor, and never more than the debt itself. Rounded up to kopiyky. */
    public BigDecimal minimumPayment(BigDecimal closingDebt) {

        if (closingDebt.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal share = closingDebt.multiply(properties.minimumPaymentPercent())
                .divide(HUNDRED, 2, RoundingMode.CEILING);

        return share.max(properties.minimumPaymentFloor()).min(closingDebt).setScale(2, RoundingMode.CEILING);
    }

    public SettlementDto settle(BigDecimal closingDebt, BigDecimal minimumPayment, BigDecimal repaid) {

        BigDecimal unpaid = closingDebt.subtract(repaid).max(BigDecimal.ZERO);

        if (unpaid.signum() == 0) {
            return new SettlementDto(CreditStatementStatus.PAID, money(BigDecimal.ZERO), money(BigDecimal.ZERO));
        }

        BigDecimal interest = unpaid.multiply(properties.monthlyInterestRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        if (repaid.compareTo(minimumPayment) >= 0) {
            return new SettlementDto(CreditStatementStatus.MINIMUM_PAID, interest, money(BigDecimal.ZERO));
        }

        return new SettlementDto(CreditStatementStatus.OVERDUE, interest, money(properties.lateFee()));
    }

    private static BigDecimal money(BigDecimal amount) {

        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
