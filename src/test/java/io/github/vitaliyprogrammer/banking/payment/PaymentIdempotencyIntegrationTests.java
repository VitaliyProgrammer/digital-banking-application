package io.github.vitaliyprogrammer.banking.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exception.IdempotencyKeyReusedException;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Proof tests for phase 2: a repeated request never pays twice, and fees are posted with the transfer. */
class PaymentIdempotencyIntegrationTests extends IntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Test
    void parallelRequestsWithSameKeyCreateOnePayment() throws Exception {
        UUID payerId = createUser();
        UUID payerAccount = openAccountWithBalance(payerId, "100.00");
        AccountResponseDto recipient = accountService.open(createUser(), new OpenAccountRequestDto(Currency.UAH));

        String key = UUID.randomUUID().toString();
        TransferRequestDto request = new TransferRequestDto(
                payerAccount, recipient.number(), new BigDecimal("10.00"), "Rent");

        // A client whose network retried the same request 20 times at once.
        List<PaymentResponseDto> responses = runConcurrently(20, i -> () -> paymentService.transfer(payerId, key, request));

        assertThat(responses).extracting(PaymentResponseDto::id).containsOnly(responses.getFirst().id());
        assertThat(paymentsWithKey(payerId, key)).isEqualTo(1);
        assertThat(balanceOf(payerAccount)).isEqualByComparingTo("85.00");   // 100 - 10 - 5.00 minimum fee
        assertThat(balanceOf(recipient.id())).isEqualByComparingTo("10.00");
    }

    /**
     * Regression test: a payment reads its accounts before the ledger locks them. Without refreshing them
     * after the lock, parallel payments would compute from a stale balance and lose updates.
     */
    @Test
    void parallelPaymentsWithDifferentKeysNeverLoseUpdates() throws Exception {
        UUID payerId = createUser();
        UUID payerAccount = openAccountWithBalance(payerId, "1000.00");
        AccountResponseDto recipient = accountService.open(createUser(), new OpenAccountRequestDto(Currency.UAH));

        runConcurrently(20, i -> () -> paymentService.transfer(payerId, UUID.randomUUID().toString(),
                new TransferRequestDto(payerAccount, recipient.number(), new BigDecimal("10.00"), null)));

        assertThat(balanceOf(payerAccount)).isEqualByComparingTo("700.00");    // 1000 - 20 × (10 + 5.00 fee)
        assertThat(balanceOf(recipient.id())).isEqualByComparingTo("200.00");
    }

    @Test
    void sameKeyWithDifferentRequestIsRejected() {
        UUID payerId = createUser();
        UUID payerAccount = openAccountWithBalance(payerId, "100.00");
        String recipientNumber = accountService.open(createUser(), new OpenAccountRequestDto(Currency.UAH)).number();
        String key = UUID.randomUUID().toString();

        paymentService.transfer(payerId, key,
                new TransferRequestDto(payerAccount, recipientNumber, new BigDecimal("10.00"), null));

        assertThatThrownBy(() -> paymentService.transfer(payerId, key,
                new TransferRequestDto(payerAccount, recipientNumber, new BigDecimal("20.00"), null)))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        assertThat(balanceOf(payerAccount)).isEqualByComparingTo("85.00");
    }

    @Test
    void transferPostsAmountAndFeeInOneBalancedTransaction() {
        UUID payerId = createUser();
        UUID payerAccount = openAccountWithBalance(payerId, "3000.00");
        AccountResponseDto recipient = accountService.open(createUser(), new OpenAccountRequestDto(Currency.UAH));
        // Fee income is split into shards (V22): the bank's income is the sum of all of them.
        BigDecimal feeIncomeBefore = feeIncome();

        // Spaces in the IBAN, as copied from a banking app.
        String spacedNumber = recipient.number().replaceAll("(.{4})", "$1 ").trim();
        PaymentResponseDto payment = paymentService.transfer(payerId, UUID.randomUUID().toString(),
                new TransferRequestDto(payerAccount, spacedNumber, new BigDecimal("2000.00"), null));

        assertThat(payment.fee()).isEqualByComparingTo("10.00");             // 0.5% of 2000.00
        assertThat(balanceOf(payerAccount)).isEqualByComparingTo("990.00");  // 3000 - 2000 - 10
        assertThat(balanceOf(recipient.id())).isEqualByComparingTo("2000.00");
        assertThat(feeIncome().subtract(feeIncomeBefore)).isEqualByComparingTo("10.00");
        assertThat(entriesOfPayment(payment.id())).isEqualTo(3);
    }

    private BigDecimal feeIncome() {
        return jdbcTemplate.queryForObject(
                "SELECT sum(balance) FROM accounts WHERE type = 'FEE_INCOME' AND currency = 'UAH'", BigDecimal.class);
    }

    private UUID openAccountWithBalance(UUID ownerId, String amount) {
        UUID accountId = accountService.open(ownerId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(ownerId, UUID.randomUUID().toString(), new TopUpRequestDto(accountId, new BigDecimal(amount)));
        return accountId;
    }

    private Integer paymentsWithKey(UUID initiatorId, String key) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE initiator_id = ? AND idempotency_key = ?",
                Integer.class, initiatorId, key);
    }

    private Integer entriesOfPayment(UUID paymentId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM ledger_entries e
                JOIN payments p ON p.ledger_transaction_id = e.transaction_id
                WHERE p.id = ?
                """, Integer.class, paymentId);
    }
}
