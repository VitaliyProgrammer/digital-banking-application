package io.github.vitaliyprogrammer.banking.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 10.2. A committed payment is counted once, a rolled-back one never;
 * the queue gauges read the real state of their tables.
 */
class BusinessMetricsIntegrationTests extends IntegrationTest {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Test
    void aCommittedPaymentIsCountedOnceAndARolledBackOneNever() {
        double topUpsBefore = payments("top_up");
        double transfersBefore = payments("transfer");

        UUID userId = createUser();
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        String key = UUID.randomUUID().toString();
        paymentService.topUp(userId, key, new TopUpRequestDto(account, new BigDecimal("100.00")));
        // The same request again: an idempotent replay, not a second payment.
        paymentService.topUp(userId, key, new TopUpRequestDto(account, new BigDecimal("100.00")));

        UUID otherUser = createUser();
        UUID otherAccount = accountService.open(otherUser, new OpenAccountRequestDto(Currency.UAH)).id();
        String iban = accountService.getAccount(userId, account).number();
        assertThatThrownBy(() -> paymentService.transfer(otherUser, UUID.randomUUID().toString(),
                new TransferRequestDto(otherAccount, iban, new BigDecimal("500.00"), null)))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(payments("top_up") - topUpsBefore).isEqualTo(1.0);
        assertThat(payments("transfer") - transfersBefore).isZero();
    }

    @Test
    void theOutboxGaugeReadsTheRealBacklog() {
        UUID userId = createUser();
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(userId, UUID.randomUUID().toString(), new TopUpRequestDto(account, new BigDecimal("1.00")));

        Integer unpublished = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE published_at IS NULL", Integer.class);
        assertThat(registry.get("bank.outbox.unpublished").gauge().value()).isEqualTo(unpublished.doubleValue());
        assertThat(registry.get("bank.outbox.oldest.unpublished.age").gauge().value()).isGreaterThanOrEqualTo(0.0);
        assertThat(registry.get("bank.notifications.emails.pending").gauge().value()).isGreaterThanOrEqualTo(0.0);
    }

    private double payments(String type) {
        Counter counter = registry.find("bank.payments").tag("type", type).tag("result", "completed").counter();
        return counter != null ? counter.count() : 0.0;
    }
}
