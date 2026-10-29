package io.github.vitaliyprogrammer.banking.credit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.configuration.CreditProperties;
import io.github.vitaliyprogrammer.banking.dto.response.ScoringResultDto;
import io.github.vitaliyprogrammer.banking.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.service.impl.CreditScoring;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * The credit rules alone: no Spring, no database, milliseconds per test. Every rule and every border
 * gets its own case, which would be far too slow to cover with integration tests.
 * Settings as in application.yml: 30 days, 90-day window, at least 15 000, 50% share,
 * auto-approve up to 10 000, review up to 1.5 × affordable, maximum 100 000.
 */
class CreditScoringTests {

    private final CreditScoring scoring = new CreditScoring(new CreditProperties(
            30, 90, new BigDecimal("15000.00"), new BigDecimal("0.50"),
            new BigDecimal("10000.00"), new BigDecimal("1.50"), new BigDecimal("100000.00")));

    @Test
    void aNewAccountIsDeclinedWhateverItEarns() {
        ScoringResultDto result = scoring.score(10, money("500000"), money("1000"));

        assertThat(result.status()).isEqualTo(CreditApplicationStatus.DECLINED);
        assertThat(result.reasons()).anySatisfy(reason -> assertThat(reason).contains("10 дн."));
    }

    @Test
    void tooLittleIncomeIsDeclined() {
        assertThat(scoring.score(365, money("14999.99"), money("1000")).status())
                .isEqualTo(CreditApplicationStatus.DECLINED);
    }

    @Test
    void affordableLimitIsHalfOfTheMonthlyIncomeInWholeHundreds() {
        // 61 000 in 90 days = 20 333.33 a month; half of it = 10 166.66 → 10 100.
        assertThat(scoring.score(365, money("61000"), money("1000")).affordableLimit())
                .isEqualByComparingTo("10100.00");
        // Never above the bank's maximum, however high the income.
        assertThat(scoring.score(365, money("9000000"), money("1000")).affordableLimit())
                .isEqualByComparingTo("100000.00");
    }

    @Test
    void aSmallAffordableRequestIsApprovedAutomatically() {
        // 60 000 in 90 days → affordable 10 000.
        ScoringResultDto result = scoring.score(365, money("60000"), money("8000"));

        assertThat(result.status()).isEqualTo(CreditApplicationStatus.APPROVED);
        assertThat(result.approvedLimit()).isEqualByComparingTo("8000");
    }

    @Test
    void anAffordableButLargeRequestGoesToAPerson() {
        // 120 000 in 90 days → affordable 20 000, but 15 000 is above the auto-approve threshold.
        assertThat(scoring.score(365, money("120000"), money("15000")).status())
                .isEqualTo(CreditApplicationStatus.MANUAL_REVIEW);
    }

    @Test
    void aRequestSomewhatAboveAffordableGoesToAPersonAndFarAboveIsDeclined() {
        // Affordable 10 000: up to 15 000 a person decides, above that the rules say no.
        assertThat(scoring.score(365, money("60000"), money("15000")).status())
                .isEqualTo(CreditApplicationStatus.MANUAL_REVIEW);
        assertThat(scoring.score(365, money("60000"), money("15000.01")).status())
                .isEqualTo(CreditApplicationStatus.DECLINED);
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
