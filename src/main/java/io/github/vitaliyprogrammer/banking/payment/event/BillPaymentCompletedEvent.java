package io.github.vitaliyprogrammer.banking.payment.event;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.entity.status.ProviderCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * "A bill payment reached the provider and is final." Published by the payment module inside the transaction
 * that completes the payment; any module that cares (cashback today, notifications later) listens to it,
 * and the payment module knows nothing about them.
 */
public record BillPaymentCompletedEvent(
        UUID paymentId,
        UUID userId,
        UUID accountId,
        ProviderCategory category,
        Currency currency,
        BigDecimal amount,
        Instant completedAt
) {
}
