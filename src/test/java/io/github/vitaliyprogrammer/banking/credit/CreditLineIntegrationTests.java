package io.github.vitaliyprogrammer.banking.credit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.exception.CreditLineNotFoundException;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.card.dto.request.IssueCardRequestDto;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.card.service.CardService;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditDecisionRequestDto;
import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditApplicationResponseDto;
import io.github.vitaliyprogrammer.banking.credit.dto.response.CreditLineResponseDto;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.credit.exception.UniversalCardRequiredException;
import io.github.vitaliyprogrammer.banking.credit.service.CreditApplicationService;
import io.github.vitaliyprogrammer.banking.credit.service.CreditLineService;
import io.github.vitaliyprogrammer.banking.credit.service.impl.GracePeriod;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.CreditRepaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.SavingsTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.exception.RepaymentNotNeededException;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.savings.service.SavingsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for the credit limit on the UNIVERSAL card, the way monobank does it. One account carries both the
 * client's own money and the limit: an approval adds the limit to it (no new card), spending uses own money first
 * and then the limit, parallel spending never goes past it, credit money pays cards and bills but never leaves by
 * IBAN or goes into a jar, and any money that arrives on the account pays the debt off by itself.
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

    private UUID universalCard;

    private String recipientCardNumber;

    private String recipientIban;

    /** A client with 60 000 of income (affordable 10 000) and a UNIVERSAL card, and a stranger to pay. */
    @BeforeEach
    void clientAndRecipient() {
        clientId = createUser();
        currentAccount = establishedAccount(clientId);
        universalCard = cardService.issue(clientId, new IssueCardRequestDto(currentAccount, CardType.UNIVERSAL)).id();

        UUID recipientId = createUser();
        UUID recipientAccount = accountService.open(recipientId, new OpenAccountRequestDto(Currency.UAH)).id();
        UUID recipientCard = cardService.issue(recipientId,
                new IssueCardRequestDto(recipientAccount, CardType.PAYROLL)).id();
        recipientCardNumber = cardService.getCardDetails(recipientId, recipientCard).number();
        recipientIban = accountService.getAccount(recipientId, recipientAccount).number();
    }

    @Test
    void approvedLimitAppearsOnTheUniversalCard() {
        assertThatThrownBy(() -> creditLineService.getCreditLine(clientId))
                .isInstanceOf(CreditLineNotFoundException.class);

        apply(clientId, "8000.00");

        CreditLineResponseDto creditLine = creditLineService.getCreditLine(clientId);
        assertThat(creditLine.accountId()).isEqualTo(currentAccount);
        assertThat(creditLine.creditLimit()).isEqualByComparingTo("8000.00");
        assertThat(creditLine.ownFunds()).isEqualByComparingTo("60000.00");
        assertThat(creditLine.available()).isEqualByComparingTo("68000.00");
        assertThat(creditLine.debt()).isEqualByComparingTo("0.00");
        assertThat(cardService.getCards(clientId)).singleElement()
                .satisfies(card -> assertThat(card.type()).isEqualTo(CardType.UNIVERSAL));
    }

    @Test
    void applyingRequiresAUniversalCard() {
        UUID otherClient = createUser();
        establishedAccount(otherClient);

        assertThatThrownBy(() -> apply(otherClient, "8000.00")).isInstanceOf(UniversalCardRequiredException.class);
    }

    @Test
    void manualApprovalPutsTheLimitOnTheCard() {
        CreditApplicationResponseDto application = apply(clientId, "12000.00");
        assertThat(application.status()).isEqualTo(CreditApplicationStatus.MANUAL_REVIEW);
        assertThatThrownBy(() -> creditLineService.getCreditLine(clientId))
                .isInstanceOf(CreditLineNotFoundException.class);

        UUID reviewer = createUser();
        jdbcTemplate.update("UPDATE users SET role = 'SUPPORT' WHERE id = ?", reviewer);
        creditApplicationService.approve(reviewer, application.id(),
                new CreditDecisionRequestDto(new BigDecimal("10000.00"), "Stable salary"));

        CreditLineResponseDto creditLine = creditLineService.getCreditLine(clientId);
        assertThat(creditLine.accountId()).isEqualTo(currentAccount);
        assertThat(creditLine.creditLimit()).isEqualByComparingTo("10000.00");
    }

    @Test
    void parallelSpendingStopsAtTheLimit() throws Exception {
        apply(clientId, "8000.00");

        // 60 000 own + 8 000 limit = 68 000. 10 × (7 000 + 0.5% fee) = 70 350 wanted: exactly 9 fit (63 315).
        List<Boolean> results = runConcurrently(10, i -> () -> {
            try {
                spend("7000.00");
                return true;
            } catch (InsufficientFundsException e) {
                return false;
            }
        });

        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(9);
        CreditLineResponseDto creditLine = creditLineService.getCreditLine(clientId);
        assertThat(creditLine.ownFunds()).isEqualByComparingTo("0.00");
        assertThat(creditLine.debt()).isEqualByComparingTo("3315.00");
        assertThat(creditLine.available()).isEqualByComparingTo("4685.00");
    }

    @Test
    void creditMoneyIsForCardsAndBillsOnly() {
        apply(clientId, "8000.00");
        UUID jar = savingsService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        spend("61000.00");
        // 61 000 + 305 fee = 61 305: own 60 000 and 1 305 of the limit.

        assertThatThrownBy(() -> paymentService.transfer(clientId, newKey(),
                new TransferRequestDto(currentAccount, recipientIban, new BigDecimal("100.00"), null)))
                .isInstanceOf(InsufficientFundsException.class);
        assertThatThrownBy(() -> paymentService.depositToSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(currentAccount, new BigDecimal("100.00"))))
                .isInstanceOf(InsufficientFundsException.class);

        spend("1000.00");
        assertThat(creditLineService.getCreditLine(clientId).debt()).isEqualByComparingTo("2310.00");
    }

    @Test
    void incomingMoneyRepaysTheDebt() {
        apply(clientId, "8000.00");
        spend("63000.00");
        // 63 000 + 315 fee = 63 315: own 60 000, 3 315 owed.

        assertThatThrownBy(() -> paymentService.repayCredit(clientId, newKey(),
                new CreditRepaymentRequestDto(currentAccount, new BigDecimal("1000.00"))))
                .isInstanceOf(RepaymentNotNeededException.class);
        paymentService.topUp(clientId, newKey(), new TopUpRequestDto(currentAccount, new BigDecimal("1315.00")));

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

    private void spend(String amount) {
        paymentService.cardTransfer(clientId, newKey(),
                new CardTransferRequestDto(universalCard, recipientCardNumber, new BigDecimal(amount), null));
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
