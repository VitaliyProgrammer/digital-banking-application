package io.github.vitaliyprogrammer.banking.payment.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.entity.Payment;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponseDto(
        UUID id,
        PaymentType type,
        PaymentStatus status,
        UUID fromAccountId,
        UUID toAccountId,
        UUID fromCardId,
        UUID toCardId,
        Currency currency,
        BigDecimal amount,
        BigDecimal fee,
        String description,
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
                payment.getCurrency(),
                payment.getAmount().setScale(2, RoundingMode.HALF_EVEN),
                payment.getFee().setScale(2, RoundingMode.HALF_EVEN),
                payment.getDescription(),
                payment.getCreatedAt(),
                payment.getCompletedAt());
    }
}
