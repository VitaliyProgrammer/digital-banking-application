package io.github.vitaliyprogrammer.banking.payment.event;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.entity.Payment;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The body of payment.completed and payment.failed. A contract with consumers: fields may be added later,
 * never renamed or removed. No account numbers or card data: a message goes to a broker, not into a vault.
 */
public record PaymentMessage(
        UUID paymentId,
        UUID userId,
        PaymentType type,
        PaymentStatus status,
        Currency currency,
        BigDecimal amount,
        BigDecimal fee,
        String description,
        String failureReason,
        Instant occurredAt
) {

    public static PaymentMessage from(Payment payment) {
        return new PaymentMessage(
                payment.getId(),
                payment.getInitiatorId(),
                payment.getType(),
                payment.getStatus(),
                payment.getCurrency(),
                payment.getAmount(),
                payment.getFee(),
                payment.getDescription(),
                payment.getFailureReason(),
                payment.getCompletedAt());
    }
}
