package io.github.vitaliyprogrammer.banking.configuration;

import io.github.vitaliyprogrammer.banking.entity.status.ProviderCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.cashback.*". */
@Validated
@ConfigurationProperties(prefix = "banking.cashback")
public record CashbackProperties(
        /* Off in tests, which call the payout themselves. */
        boolean jobsEnabled,

        @NotBlank
        String payoutCron,

        /* Percent of the payment per provider category, e.g. MOBILE: 5.00. A category missing here earns nothing. */
        @NotEmpty
        Map<ProviderCategory, BigDecimal> rates,

        /* Most a client can earn in one month, in UAH. */
        @NotNull
        @DecimalMin("0.01")
        BigDecimal monthlyCap
) {

    public BigDecimal rate(ProviderCategory category) {
        return rates.getOrDefault(category, BigDecimal.ZERO);
    }
}
