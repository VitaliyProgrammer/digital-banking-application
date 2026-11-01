package io.github.vitaliyprogrammer.banking.exchange.entity;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exchange.entity.status.RateMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * The bank's buy/sell rate of a currency from {@code validFrom} until the next row. Never updated:
 * a new rate is a new row, so the table doubles as the change history.
 */
@Entity
@Table(name = "bank_rates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankRate {

    public static final int SCALE = 4;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    /** The bank buys the currency from the client at this rate (UAH per 1 unit). */
    private BigDecimal buy;

    /** The bank sells the currency to the client at this rate. */
    private BigDecimal sell;

    @Enumerated(EnumType.STRING)
    private RateMode mode;

    private BigDecimal spreadPercent;

    /** The NBU rate at the moment this rate was set: shows how far from the official rate the bank was. */
    private BigDecimal officialRate;

    /** The admin who set it; null when the system did. */
    private UUID setBy;

    private String comment;

    private Instant validFrom;

    /**
     * Buy below and sell above the official rate by the same percentage. Rounding always goes in the bank's
     * favour (buy down, sell up), so a rate never ends up a fraction of a kopiyka on the wrong side.
     */
    public static BankRate fromSpread(Currency currency, BigDecimal officialRate, BigDecimal spreadPercent,
                                      UUID setBy, String comment, Instant now) {
        BigDecimal share = spreadPercent.divide(HUNDRED);
        BankRate rate = new BankRate();
        rate.buy = officialRate.multiply(BigDecimal.ONE.subtract(share)).setScale(SCALE, RoundingMode.FLOOR);
        rate.sell = officialRate.multiply(BigDecimal.ONE.add(share)).setScale(SCALE, RoundingMode.CEILING);
        rate.mode = RateMode.SPREAD;
        rate.spreadPercent = spreadPercent;
        return rate.fill(currency, officialRate, setBy, comment, now);
    }

    public static BankRate manual(Currency currency, BigDecimal buy, BigDecimal sell, BigDecimal officialRate,
                                  UUID setBy, String comment, Instant now) {
        BankRate rate = new BankRate();
        rate.buy = buy.setScale(SCALE, RoundingMode.UNNECESSARY);
        rate.sell = sell.setScale(SCALE, RoundingMode.UNNECESSARY);
        rate.mode = RateMode.MANUAL;
        return rate.fill(currency, officialRate, setBy, comment, now);
    }

    /** The same spread applied to a new official rate, e.g. after the daily NBU update. */
    public BankRate recalculate(BigDecimal newOfficialRate, Instant now) {
        return fromSpread(currency, newOfficialRate, spreadPercent, null, "Recalculated from the new NBU rate", now);
    }

    public boolean followsOfficialRate() {
        return mode == RateMode.SPREAD;
    }

    private BankRate fill(Currency currency, BigDecimal officialRate, UUID setBy, String comment, Instant now) {
        this.currency = currency;
        this.officialRate = officialRate;
        this.setBy = setBy;
        this.comment = comment;
        this.validFrom = now;
        return this;
    }
}
