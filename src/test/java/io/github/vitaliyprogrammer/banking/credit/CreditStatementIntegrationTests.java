package io.github.vitaliyprogrammer.banking.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.card.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.card.dto.response.CardResponseDto;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditLineResponseDto;
import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditStatementResponseDto;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditStatementStatus;
import io.github.vitaliyprogrammer.banking.credit.service.CreditApplicationService;
import io.github.vitaliyprogrammer.banking.credit.service.CreditLineService;
import io.github.vitaliyprogrammer.banking.credit.service.impl.GracePeriod;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CreditRepaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 8.3. Closing a month snapshots the debt once; after the due date a statement is settled
 * once: repaid in full costs nothing, the minimum payment avoids the late fee, less than that is overdue;
 * and the bank's charges may take a used-up credit line past its limit, while spending may not.
 */
class CreditStatementIntegrationTests extends IntegrationTest {

    /** A month far in the past, so a statement made up by a test never meets one the job closes. */
    private static final String MADE_UP_PERIOD = "2000-01";

    @Autowired
    private CreditApplicationService creditApplicationService;

    @Autowired
    private CreditLineService creditLineService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CardService cardService;

    @Autowired
    private PaymentService paymentService;

    private final LocalDate today = LocalDate.now(GracePeriod.KYIV);

    private UUID clientId;

    private UUID currentAccount;

    private UUID creditCard;

    private String recipientCardNumber;

    /** A client with a credit line of 8 000, and a stranger with a card to spend the credit on. */
    @BeforeEach
    void clientWithACreditLine() {
        clientId = createUser();
        currentAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        jdbcTemplate.update("UPDATE accounts SET created_at = now() - interval '60 days' WHERE id = ?", currentAccount);
        paymentService.topUp(clientId, newKey(), new TopUpRequestDto(currentAccount, new BigDecimal("60000.00")));
        creditApplicationService.apply(clientId, new CreditApplicationRequestDto(new BigDecimal("8000.00")));
        creditCard = cardService.getCards(clientId).stream()
                .filter(card -> card.type() == CardType.CREDIT)
                .map(CardResponseDto::id)
                .findFirst()
                .orElseThrow();

        UUID recipientId = createUser();
        UUID recipientAccount = accountService.open(recipientId, new OpenAccountRequestDto(Currency.UAH)).id();
        UUID recipientCard = cardService.issue(recipientId,
                new IssueCardRequestDto(recipientAccount, CardType.PAYROLL)).id();
        recipientCardNumber = cardService.getCardDetails(recipientId, recipientCard).number();
    }

    @Test
    void closingTheMonthSnapshotsTheDebtWithItsMinimumPaymentOnce() {
        spend("3000.00");

        LocalDate firstOfNextMonth = today.plusMonths(1).withDayOfMonth(1);
        creditLineService.closeMonth(firstOfNextMonth);
        creditLineService.closeMonth(firstOfNextMonth);

        assertThat(creditLineService.getStatements(clientId)).singleElement().satisfies(statement -> {
            assertThat(statement.period()).isEqualTo(today.toString().substring(0, 7));
            assertThat(statement.closingDebt()).isEqualByComparingTo("3060.00");
            assertThat(statement.minimumPayment()).isEqualByComparingTo("153.00");
            assertThat(statement.dueDate()).isEqualTo(GracePeriod.interestFreeUntil(today));
            assertThat(statement.status()).isEqualTo(CreditStatementStatus.OPEN);
        });
    }

    @Test
    void repaidInFullByTheDueDateCostsNothing() {
        spend("3000.00");
        statementDueTonight("3060.00", "153.00");
        repay("3060.00");

        settleTomorrow();

        assertThat(madeUpStatement().status()).isEqualTo(CreditStatementStatus.PAID);
        assertThat(creditLineService.getCreditLine(clientId).debt()).isEqualByComparingTo("0.00");
    }

    @Test
    void theMinimumPaymentAvoidsTheLateFeeAndInterestIsChargedOnlyOnceOnTheRest() {
        spend("3000.00");
        statementDueTonight("3060.00", "153.00");
        repay("200.00");

        settleTomorrow();
        settleTomorrow();

        CreditStatementResponseDto statement = madeUpStatement();
        assertThat(statement.status()).isEqualTo(CreditStatementStatus.MINIMUM_PAID);
        assertThat(statement.repaid()).isEqualByComparingTo("200.00");
        assertThat(statement.interest()).isEqualByComparingTo("71.50");
        assertThat(statement.lateFee()).isEqualByComparingTo("0.00");
        // 2 860 left + 71.50 interest, charged once although the job ran twice.
        assertThat(creditLineService.getCreditLine(clientId).debt()).isEqualByComparingTo("2931.50");
    }

    @Test
    void chargesMayTakeAUsedUpCreditLinePastItsLimitButSpendingMayNot() {
        for (int i = 0; i < 7; i++) {
            spend("1000.00");
        }
        spend("800.00");
        // 7 × 1 020 + 816 = 7 956 of 8 000 used, nothing repaid by the due date.
        statementDueTonight("7956.00", "397.80");

        settleTomorrow();

        CreditStatementResponseDto statement = madeUpStatement();
        assertThat(statement.status()).isEqualTo(CreditStatementStatus.OVERDUE);
        assertThat(statement.interest()).isEqualByComparingTo("198.90");
        assertThat(statement.lateFee()).isEqualByComparingTo("100.00");

        CreditLineResponseDto creditLine = creditLineService.getCreditLine(clientId);
        assertThat(creditLine.debt()).isEqualByComparingTo("8254.90");
        assertThat(creditLine.available()).isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> spend("1.00")).isInstanceOf(InsufficientFundsException.class);

        // Paying back into a line that is over its limit must always work.
        repay("500.00");
        assertThat(creditLineService.getCreditLine(clientId).debt()).isEqualByComparingTo("7754.90");
    }

    private void spend(String amount) {
        paymentService.cardTransfer(clientId, newKey(),
                new CardTransferRequestDto(creditCard, recipientCardNumber, new BigDecimal(amount), null));
    }

    private void repay(String amount) {
        paymentService.repayCredit(clientId, newKey(),
                new CreditRepaymentRequestDto(currentAccount, new BigDecimal(amount)));
    }

    /**
     * A real statement closes on the 1st and falls due a month later, which a test cannot wait for. So this one
     * "closed" a moment ago (after the spending, before the repayments) and falls due at midnight tonight;
     * the job then runs "tomorrow". The settlement code cannot tell it from a real one.
     */
    private void statementDueTonight(String closingDebt, String minimumPayment) {
        jdbcTemplate.update("""
                INSERT INTO credit_statements (id, account_id, period, closing_debt, minimum_payment, closed_at,
                                               due_date, due_at, status, created_at)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, 'OPEN', now())
                """,
                creditLineService.getCreditLine(clientId).accountId(), MADE_UP_PERIOD,
                new BigDecimal(closingDebt), new BigDecimal(minimumPayment), Timestamp.from(Instant.now()),
                today, Timestamp.from(today.plusDays(1).atStartOfDay(GracePeriod.KYIV).toInstant()));
    }

    private void settleTomorrow() {
        creditLineService.closeMonth(today.plusDays(1));
    }

    private CreditStatementResponseDto madeUpStatement() {
        return creditLineService.getStatements(clientId).stream()
                .filter(statement -> statement.period().equals(MADE_UP_PERIOD))
                .findFirst()
                .orElseThrow();
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
