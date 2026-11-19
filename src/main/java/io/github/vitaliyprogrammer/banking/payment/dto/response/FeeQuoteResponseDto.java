package io.github.vitaliyprogrammer.banking.payment.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import java.math.BigDecimal;

/**
 * What a payment will cost before the client confirms it: the same tariff the payment itself uses, so the screen
 * never shows one fee and charges another. {@code total} is what leaves the sender's account.
 */
public record FeeQuoteResponseDto(
        Currency currency,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal total
) {

    public static FeeQuoteResponseDto of(Currency currency, BigDecimal amount, BigDecimal fee) {

        return new FeeQuoteResponseDto(currency, amount, fee, amount.add(fee));
    }
}
