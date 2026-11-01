package io.github.vitaliyprogrammer.banking.payment.dto.request;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.payment.entity.status.ProviderCategory;
import java.math.BigDecimal;

/**
 * Everything a tariff may depend on. A record instead of a growing parameter list:
 * a new factor (e.g. client segment) is a new field, and existing policies simply ignore it.
 */
public record FeeRequest(
        BigDecimal amount,
        Currency currency,
        /* Card the money is sent from; null for payments without a card. */
        CardType cardType,
        /* What a bill payment pays for; null for every other payment. */
        ProviderCategory providerCategory
) {

    public static FeeRequest withoutCard(BigDecimal amount, Currency currency) {
        return new FeeRequest(amount, currency, null, null);
    }

    public static FeeRequest fromCard(BigDecimal amount, Currency currency, CardType cardType) {
        return new FeeRequest(amount, currency, cardType, null);
    }

    public static FeeRequest forBill(BigDecimal amount, Currency currency, ProviderCategory providerCategory) {
        return new FeeRequest(amount, currency, null, providerCategory);
    }
}
