package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
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
 * The bank's promise: "sell {@code sellAmount} and get exactly {@code buyAmount}, if you agree before
 * {@code expiresAt}". Without it the rate could change between the client seeing the price and pressing the button.
 */
@Entity
@Table(name = "exchange_quotes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeQuote {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID ownerId;

    private UUID fromAccountId;

    private UUID toAccountId;

    @Enumerated(EnumType.STRING)
    private Currency fromCurrency;

    @Enumerated(EnumType.STRING)
    private Currency toCurrency;

    @Column(precision = 19, scale = 4)
    private BigDecimal sellAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal buyAmount;

    @Column(precision = 19, scale = 8)
    private BigDecimal rate;

    private Instant createdAt;

    private Instant expiresAt;

    public static ExchangeQuote create(UUID ownerId,
                                       UUID fromAccountId,
                                       UUID toAccountId,
                                       Currency fromCurrency,
                                       Currency toCurrency,
                                       BigDecimal sellAmount,
                                       BigDecimal buyAmount,
                                       BigDecimal rate,
                                       Instant now,
                                       Instant expiresAt) {
        ExchangeQuote quote = new ExchangeQuote();
        quote.ownerId = ownerId;
        quote.fromAccountId = fromAccountId;
        quote.toAccountId = toAccountId;
        quote.fromCurrency = fromCurrency;
        quote.toCurrency = toCurrency;
        quote.sellAmount = sellAmount;
        quote.buyAmount = buyAmount;
        quote.rate = rate;
        quote.createdAt = now;
        quote.expiresAt = expiresAt;
        return quote;
    }

    /** The end is exclusive: at exactly {@code expiresAt} the quote is already gone. */
    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
