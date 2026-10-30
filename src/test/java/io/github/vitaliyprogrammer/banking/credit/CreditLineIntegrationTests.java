package io.github.vitaliyprogrammer.banking.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.CreditDecisionRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.CreditRepaymentRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.SavingsTransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CardResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditApplicationResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditLineResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.exception.CreditLineNotFoundException;
import io.github.vitaliyprogrammer.banking.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.exception.RepaymentExceedsDebtException;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CardService;
import io.github.vitaliyprogrammer.banking.service.CreditApplicationService;
import io.github.vitaliyprogrammer.banking.service.CreditLineService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import io.github.vitaliyprogrammer.banking.service.SavingsService;
import io.github.vitaliyprogrammer.banking.service.impl.GracePeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 8.2. An approved limit becomes a credit account with a CREDIT card in the same transaction,
 * parallel spending never goes past the limit, borrowed money cannot be parked in savings, a repayment is never
 * larger than the debt, and the last closed statement is what must be repaid to stay interest-free.
 */
class CreditLineIntegrationTests extends IntegrationTest {

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

    @Autowired
    private SavingsService savingsService;

    private UUID clientId;

    private UUID currentAccount;

    private String recipientCardNumber;

    /** A client with 60 000 of income (affordable 10 000), and a stranger with a card to send money to. */
    @BeforeEach
    void clientAndRecipient() {
        clientId = createUser();
        currentAccount = establishedAccount(clientId);

        UUID recipientId = createUser();
        UUID recipientAccount = accountService.open(recipientId, new OpenAccountRequestDto(Currency.UAH)).id();
        UUID recipientCard = cardService.issue(recipientId,
                new IssueCardRequestDto(recipientAccount, CardType.PAYROLL)).id();
        recipientCardNumber = cardService.getCardDetails(recipientId, recipientCard).number();
    }

    @Test
    void anApprovedLimitBecomesACreditLineWithACreditCard() {
        assertThatThrownBy(() -> creditLineService.getCreditLine(clientId))
                .isInstanceOf(CreditLineNotFoundException.class);

        apply(clientId, "8000.00");

        CreditLineResponseDto creditLine = creditLineService.getCreditLine(clientId);
        assertThat(creditLine.creditLimit()).isEqualByComparingTo("8000.00");
        assertThat(creditLine.available()).isEqualByComparingTo("8000.00");
        assertThat(creditLine.debt()).isEqualByComparingTo("0.00");
        assertThat(cardService.getCards(clientId)).singleElement().satisfies(card -> {
            assertThat(card.type()).isEqualTo(CardType.CREDIT);
            assertThat(card.accountId()).isEqualTo(creditLine.accountId());
        });
    }

    @Test
    void aManualApprovalOpensTheCreditLineToo() {
        CreditApplicationResponseDto application = apply(clientId, "12000.00");
        assertThat(application.status()).isEqualTo(CreditApplicationStatus.MANUAL_REVIEW);
        assertThatThrownBy(() -> creditLineService.getCreditLine(clientId))
                .isInstanceOf(CreditLineNotFoundException.class);

        UUID reviewer = createUser();
        jdbcTemplate.update("UPDATE users SET role = 'SUPPORT' WHERE id = ?", reviewer);
        creditApplicationService.approve(reviewer, application.id(),
                new CreditDecisionRequestDto(new BigDecimal("10000.00"), "Stable salary"));

        assertThat(creditLineService.getCreditLine(clientId).creditLimit()).isEqualByComparingTo("10000.00");
    }

    @Test
    void parallelSpendingNeverGoesPastTheLimit() throws Exception {
        apply(clientId, "8000.00");
        UUID creditCard = creditCard();

        // 10 × (1 000 + 2% fee) = 10 200 wanted, 8 000 allowed: exactly 7 fit (7 140), the 8th would need 8 160.
        List<Boolean> results = runConcurrently(10, i -> () -> {
            try {
                paymentService.cardTransfer(clientId, UUID.randomUUID().toString(),
                        new CardTransferRequestDto(creditCard, recipientCardNumber, new BigDecimal("1000.00"), null));
                return true;
            } catch (InsufficientFundsException e) {
                return false;
            }
        });

        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(7);
        CreditLineResponseDto creditLine = creditLineService.getCreditLine(clientId);
        assertThat(creditLine.debt()).isEqualByComparingTo("7140.00");
        assertThat(creditLine.available()).isEqualByComparingTo("860.00");
    }

    @Test
    void borrowedMoneyCannotBeMovedIntoSavingsOrSentByIban() {
        apply(clientId, "8000.00");
        UUID creditAccount = creditLineService.getCreditLine(clientId).accountId();
        UUID jar = savingsService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        String someIban = accountService.getAccount(clientId, currentAccount).number();

        assertThatThrownBy(() -> paymentService.depositToSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(creditAccount, new BigDecimal("1000.00"))))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> paymentService.transfer(clientId, newKey(),
                new TransferRequestDto(creditAccount, someIban, new BigDecimal("1000.00"), null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void aRepaymentIsNeverLargerThanTheDebtAndTheClosedStatementIsWhatKeepsItInterestFree() {
        apply(clientId, "8000.00");
        paymentService.cardTransfer(clientId, newKey(),
                new CardTransferRequestDto(creditCard(), recipientCardNumber, new BigDecimal("3000.00"), null));
        // 3 000 + 60 fee = 3 060 owed.

        assertThatThrownBy(() -> repay("3060.01")).isInstanceOf(RepaymentExceedsDebtException.class);
        repay("1060.00");

        LocalDate today = LocalDate.now(GracePeriod.KYIV);
        CreditLineResponseDto now = creditLineService.getCreditLine(clientId, today);
        assertThat(now.debt()).isEqualByComparingTo("2000.00");
        // Spent this month: not in a closed statement yet, interest-free until the end of next month.
        assertThat(now.statementDebt()).isEqualByComparingTo("0.00");
        assertThat(now.newSpendingInterestFreeUntil()).isEqualTo(GracePeriod.interestFreeUntil(today));

        // A month later this month's statement has closed: its 2 000 must be repaid by the end of that month.
        LocalDate nextMonth = today.plusMonths(1).withDayOfMonth(10);
        CreditLineResponseDto later = creditLineService.getCreditLine(clientId, nextMonth);
        assertThat(later.statementDebt()).isEqualByComparingTo("2000.00");
        assertThat(later.leftToRepayInterestFree()).isEqualByComparingTo("2000.00");
        assertThat(later.statementInterestFreeUntil()).isEqualTo(nextMonth.withDayOfMonth(nextMonth.lengthOfMonth()));
    }

    private UUID establishedAccount(UUID userId) {
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        jdbcTemplate.update("UPDATE accounts SET created_at = now() - interval '60 days' WHERE id = ?", account);
        paymentService.topUp(userId, newKey(), new TopUpRequestDto(account, new BigDecimal("60000.00")));
        return account;
    }

    private CreditApplicationResponseDto apply(UUID userId, String limit) {
        return creditApplicationService.apply(userId, new CreditApplicationRequestDto(new BigDecimal(limit)));
    }

    private UUID creditCard() {
        return cardService.getCards(clientId).stream()
                .filter(card -> card.type() == CardType.CREDIT)
                .map(CardResponseDto::id)
                .findFirst()
                .orElseThrow();
    }

    private void repay(String amount) {
        paymentService.repayCredit(clientId, newKey(),
                new CreditRepaymentRequestDto(currentAccount, new BigDecimal(amount)));
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
