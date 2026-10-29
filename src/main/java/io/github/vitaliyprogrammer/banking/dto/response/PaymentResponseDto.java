package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.Payment;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/** {@code creditedAmount} and {@code exchangeRate} are set for exchanges only: what arrived in the other currency. */
public record PaymentResponseDto(
        UUID id,
        PaymentType type,
        PaymentStatus status,
        UUID fromAccountId,
        UUID toAccountId,
        UUID fromCardId,
        UUID toCardId,
        UUID providerId,
        String providerAccountNumber,
        Currency currency,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal creditedAmount,
        BigDecimal exchangeRate,
        String description,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {

    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getType(),
                payment.getStatus(),
                payment.getFromAccountId(),
                payment.getToAccountId(),
                payment.getFromCardId(),
                payment.getToCardId(),
                payment.getProviderId(),
                payment.getProviderAccountNumber(),
                payment.getCurrency(),
                money(payment.getAmount()),
                money(payment.getFee()),
                money(payment.getCreditedAmount()),
                payment.getExchangeRate(),
                payment.getDescription(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getCompletedAt());
    }

    private static BigDecimal money(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_EVEN) : null;
    }
}
