package io.github.vitaliyprogrammer.banking.dto.request;

import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import java.math.BigDecimal;

/**
 * Everything a tariff may depend on. A record instead of a growing parameter list:
 * a new factor (e.g. client segment) is a new field, and existing policies simply ignore it.
 */
public record FeeRequest(
        BigDecimal amount,
        Currency currency,
        /* Card the money is sent from; null for payments without a card. */
        CardType cardType
) {

    public static FeeRequest withoutCard(BigDecimal amount, Currency currency) {
        return new FeeRequest(amount, currency, null);
    }
}
