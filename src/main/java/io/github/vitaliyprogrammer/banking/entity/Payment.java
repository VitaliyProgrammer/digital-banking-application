package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * A client's request to move money, in business terms. The money itself moves in the ledger;
 * the payment keeps the link to that ledger transaction and the idempotency key of the request.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID initiatorId;

    private String idempotencyKey;

    private String requestHash;

    @Enumerated(EnumType.STRING)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private UUID fromAccountId;

    private UUID toAccountId;

    private UUID fromCardId;

    private UUID toCardId;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    private String description;

    /** The final posting: transfer, capture of a bill payment, or release of its hold. */
    private UUID ledgerTransactionId;

    private UUID providerId;

    private String providerAccountNumber;

    private UUID holdTransactionId;

    private int attempts;

    /** When the worker may pick the payment up again; also a lease, so two workers never send it at once. */
    private Instant nextAttemptAt;

    private String failureReason;

    private Instant createdAt;

    private Instant completedAt;

    public static Payment pending(UUID initiatorId,
                                  String idempotencyKey,
                                  String requestHash,
                                  PaymentType type,
                                  UUID fromAccountId,
                                  UUID toAccountId,
                                  Currency currency,
                                  BigDecimal amount,
                                  BigDecimal fee,
                                  String description,
                                  Instant now) {
        Payment payment = new Payment();
        payment.initiatorId = initiatorId;
        payment.idempotencyKey = idempotencyKey;
        payment.requestHash = requestHash;
        payment.type = type;
        payment.status = PaymentStatus.PENDING;
        payment.fromAccountId = fromAccountId;
        payment.toAccountId = toAccountId;
        payment.currency = currency;
        payment.amount = amount;
        payment.fee = fee;
        payment.description = description;
        payment.createdAt = now;
        return payment;
    }

    /** Card-to-card transfers also remember which cards were used, for the statement and disputes. */
    public void attachCards(UUID fromCardId, UUID toCardId) {

        this.fromCardId = fromCardId;
        this.toCardId = toCardId;
    }

    /** Bill payment to an external provider. It has no recipient account inside the bank. */
    public static Payment pendingBill(UUID initiatorId,
                                      String idempotencyKey,
                                      String requestHash,
                                      UUID fromAccountId,
                                      Currency currency,
                                      BigDecimal amount,
                                      BigDecimal fee,
                                      UUID providerId,
                                      String providerAccountNumber,
                                      String description,
                                      Instant now) {
        Payment payment = pending(initiatorId, idempotencyKey, requestHash, PaymentType.BILL_PAYMENT,
                fromAccountId, null, currency, amount, fee, description, now);
        payment.providerId = providerId;
        payment.providerAccountNumber = providerAccountNumber;
        return payment;
    }

    // State machine. Each transition checks where it may start from; anything else is a bug and fails loudly.

    /** PENDING → PROCESSING: the money is held, the payment waits for the worker. */
    public void startProcessing(UUID holdTransactionId, Instant now) {

        requireStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.PROCESSING;
        this.holdTransactionId = holdTransactionId;
        this.nextAttemptAt = now;
    }

    /** A worker took the payment: count the attempt and lease it until the given moment. */
    public void recordAttempt(Instant leasedUntil) {

        requireStatus(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN);
        this.attempts++;
        this.nextAttemptAt = leasedUntil;
    }

    /** PROCESSING → UNKNOWN: sent, but no answer. Reconciliation will ask the provider later. */
    public void markUnknown(Instant checkAgainAt) {

        requireStatus(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN);
        this.status = PaymentStatus.UNKNOWN;
        this.nextAttemptAt = checkAgainAt;
    }

    /** → COMPLETED: synchronous payments from PENDING, bill payments from PROCESSING or UNKNOWN. */
    public void complete(UUID ledgerTransactionId, Instant now) {

        requireStatus(PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN);
        this.status = PaymentStatus.COMPLETED;
        this.ledgerTransactionId = ledgerTransactionId;
        this.completedAt = now;
        this.nextAttemptAt = null;
    }

    /** → FAILED: the hold was released, so the client has the money back. */
    public void fail(UUID releaseTransactionId, String reason, Instant now) {

        requireStatus(PaymentStatus.PROCESSING, PaymentStatus.UNKNOWN);
        this.status = PaymentStatus.FAILED;
        this.ledgerTransactionId = releaseTransactionId;
        this.failureReason = reason;
        this.completedAt = now;
        this.nextAttemptAt = null;
    }

    public boolean isFinished() {

        return status == PaymentStatus.COMPLETED || status == PaymentStatus.FAILED;
    }

    private void requireStatus(PaymentStatus... allowed) {

        for (PaymentStatus candidate : allowed) {
            if (status == candidate) {
                return;
            }
        }

        throw new IllegalStateException("Payment " + id + " cannot change state from " + status);
    }
}
