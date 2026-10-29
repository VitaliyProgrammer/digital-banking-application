package io.github.vitaliyprogrammer.banking.configuration;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.savings.*". */
@Validated
@ConfigurationProperties(prefix = "banking.savings")
public record SavingsProperties(
        /* Off in tests, which call the accrual and the payout themselves for the dates they choose. */
        boolean jobsEnabled,

        @NotBlank
        String accrualCron,

        @NotBlank
        String payoutCron,

        /* Annual interest in percent per currency, e.g. UAH: 12.00. A currency missing here earns nothing. */
        @NotEmpty
        Map<Currency, BigDecimal> annualRates
) {

    public BigDecimal annualRate(Currency currency) {
        return annualRates.getOrDefault(currency, BigDecimal.ZERO);
    }
}
