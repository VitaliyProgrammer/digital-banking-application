package io.github.vitaliyprogrammer.banking.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.entity.Payment;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The payment's state machine. A wrong transition is a bug in our code, never a client's mistake, so it fails
 * loudly: a bill payment marked COMPLETED twice would mean money captured twice.
 */
class PaymentTests {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    @Test
    void transferCompletesStraightFromPending() {
        Payment payment = pending(PaymentType.TRANSFER);
        UUID ledgerTransactionId = UUID.randomUUID();

        payment.complete(ledgerTransactionId, NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.isFinished()).isTrue();
        assertThat(payment.getLedgerTransactionId()).isEqualTo(ledgerTransactionId);
        assertThat(payment.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void billPaymentWaitsForTheProviderAndMayBeUnknownOnTheWay() {
        Payment payment = pending(PaymentType.BILL_PAYMENT);

        payment.startProcessing(UUID.randomUUID(), NOW);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.isFinished()).isFalse();

        payment.recordAttempt(NOW.plusSeconds(30));
        payment.markUnknown(NOW.plusSeconds(60));
        payment.recordAttempt(NOW.plusSeconds(90));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(payment.getAttempts()).isEqualTo(2);

        payment.complete(UUID.randomUUID(), NOW.plusSeconds(95));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getNextAttemptAt()).as("no longer picked up by the worker").isNull();
    }

    @Test
    void rejectedBillPaymentFailsWithItsReason() {
        Payment payment = pending(PaymentType.BILL_PAYMENT);
        payment.startProcessing(UUID.randomUUID(), NOW);

        payment.fail(UUID.randomUUID(), "REJECTED", NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("REJECTED");
        assertThat(payment.isFinished()).isTrue();
    }

    @Test
    void finishedPaymentNeverChangesAgain() {
        Payment completed = pending(PaymentType.TRANSFER);
        completed.complete(UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> completed.complete(UUID.randomUUID(), NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> completed.fail(UUID.randomUUID(), "late", NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> completed.recordAttempt(NOW)).isInstanceOf(IllegalStateException.class);
        assertThat(completed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void stepsCannotBeSkippedOrRepeated() {
        Payment payment = pending(PaymentType.BILL_PAYMENT);

        // Nothing was held yet, so there is nothing to give back.
        assertThatThrownBy(() -> payment.fail(UUID.randomUUID(), "no", NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> payment.markUnknown(NOW)).isInstanceOf(IllegalStateException.class);

        payment.startProcessing(UUID.randomUUID(), NOW);
        assertThatThrownBy(() -> payment.startProcessing(UUID.randomUUID(), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Payment pending(PaymentType type) {
        return Payment.pending(UUID.randomUUID(), "key-1", "hash", type, UUID.randomUUID(), UUID.randomUUID(),
                Currency.UAH, new BigDecimal("100.00"), BigDecimal.ZERO, "Test", NOW);
    }
}
