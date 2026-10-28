package io.github.vitaliyprogrammer.banking.dto.request;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body sent to a provider. The reference is our payment id: the provider uses it to recognise a repeated
 * request, and we use it to ask about the payment's status later.
 */
public record ProviderPaymentRequestDto(
        UUID reference,
        String accountNumber,
        BigDecimal amount,
        Currency currency
) {
}
