package io.github.vitaliyprogrammer.banking.credit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.credit.configuration.CreditLineProperties;
import io.github.vitaliyprogrammer.banking.credit.dto.response.SettlementDto;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditStatementStatus;
import io.github.vitaliyprogrammer.banking.credit.service.impl.CreditTerms;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * The price of a credit line alone, no Spring and no database. Settings as in application.yml:
 * 2.5% a month, minimum payment 5% but at least 100, late fee 100.
 */
class CreditTermsTests {

    private final CreditTerms terms = new CreditTerms(new CreditLineProperties(
            false, "-", new BigDecimal("2.50"), new BigDecimal("5.00"), new BigDecimal("100.00"),
            new BigDecimal("100.00")));

    @Test
    void theMinimumPaymentIsAShareOfTheDebtButNeverBelowTheFloorOrAboveTheDebt() {
        assertThat(terms.minimumPayment(money("3060.00"))).isEqualByComparingTo("153.00");
        // 5% of 1 000 is 50, less than the floor.
        assertThat(terms.minimumPayment(money("1000.00"))).isEqualByComparingTo("100.00");
        // A debt smaller than the floor: the minimum is the whole debt, not more.
        assertThat(terms.minimumPayment(money("40.00"))).isEqualByComparingTo("40.00");
        // Kopiyky are rounded up, never down: 5% of 3 000.10 = 150.005 → 150.01.
        assertThat(terms.minimumPayment(money("3000.10"))).isEqualByComparingTo("150.01");
    }

    @Test
    void repaidInFullByTheDueDateCostsNothing() {
        SettlementDto settlement = terms.settle(money("3060.00"), money("153.00"), money("3060.00"));

        assertThat(settlement.status()).isEqualTo(CreditStatementStatus.PAID);
        assertThat(settlement.charges()).isEqualByComparingTo("0.00");
    }

    @Test
    void theMinimumPaymentAvoidsTheLateFeeButNotTheInterestOnTheRest() {
        SettlementDto settlement = terms.settle(money("3060.00"), money("153.00"), money("200.00"));

        assertThat(settlement.status()).isEqualTo(CreditStatementStatus.MINIMUM_PAID);
        // Interest only on what is still unpaid: 2.5% of 2 860.
        assertThat(settlement.interest()).isEqualByComparingTo("71.50");
        assertThat(settlement.lateFee()).isEqualByComparingTo("0.00");
    }

    @Test
    void lessThanTheMinimumIsOverdueWithInterestAndALateFee() {
        SettlementDto settlement = terms.settle(money("3060.00"), money("153.00"), money("152.99"));

        assertThat(settlement.status()).isEqualTo(CreditStatementStatus.OVERDUE);
        // 2.5% of 2 907.01 = 72.67525 → 72.68.
        assertThat(settlement.interest()).isEqualByComparingTo("72.68");
        assertThat(settlement.lateFee()).isEqualByComparingTo("100.00");
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
