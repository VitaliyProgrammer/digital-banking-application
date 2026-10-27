package io.github.vitaliyprogrammer.banking.payment.entity;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
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

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    private String description;

    private UUID ledgerTransactionId;

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

    public void complete(UUID ledgerTransactionId, Instant now) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment " + id + " is already " + status);
        }

        this.status = PaymentStatus.COMPLETED;
        this.ledgerTransactionId = ledgerTransactionId;
        this.completedAt = now;
    }
}
