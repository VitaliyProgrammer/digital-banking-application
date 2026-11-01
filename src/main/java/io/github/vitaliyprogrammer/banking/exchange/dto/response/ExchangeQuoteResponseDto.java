package io.github.vitaliyprogrammer.banking.exchange.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exchange.entity.ExchangeQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/** "Sell 100.00 USD, get 4085.08 UAH, valid until ...". Also used internally when the quote is spent. */
public record ExchangeQuoteResponseDto(
        UUID id,
        UUID fromAccountId,
        UUID toAccountId,
        Currency fromCurrency,
        Currency toCurrency,
        BigDecimal sellAmount,
        BigDecimal buyAmount,
        BigDecimal rate,
        Instant expiresAt
) {

    public static ExchangeQuoteResponseDto from(ExchangeQuote quote) {
        return new ExchangeQuoteResponseDto(
                quote.getId(),
                quote.getFromAccountId(),
                quote.getToAccountId(),
                quote.getFromCurrency(),
                quote.getToCurrency(),
                quote.getSellAmount().setScale(2, RoundingMode.UNNECESSARY),
                quote.getBuyAmount().setScale(2, RoundingMode.UNNECESSARY),
                quote.getRate(),
                quote.getExpiresAt());
    }
}
