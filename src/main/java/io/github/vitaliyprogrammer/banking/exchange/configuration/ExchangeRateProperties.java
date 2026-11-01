package io.github.vitaliyprogrammer.banking.exchange.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.exchange-rates.*". */
@Validated
@ConfigurationProperties(prefix = "banking.exchange-rates")
public record ExchangeRateProperties(
        /* Full URL of the NBU endpoint that returns today's official rates as JSON. */
        @NotBlank
        String nbuUrl,

        @NotNull
        Duration connectTimeout,

        @NotNull
        Duration readTimeout,

        /* Off in tests, which call the sync themselves. */
        boolean syncEnabled,

        @NotBlank
        String syncCron,

        /* Spread the system uses for a currency no admin has priced yet. */
        @NotNull
        @DecimalMin("0.10")
        @DecimalMax("10.00")
        BigDecimal defaultSpreadPercent,

        /* A manual rate further than this from the NBU rate is rejected: protects against a typo like 4.15 for 41.5. */
        @NotNull
        @DecimalMin("1.00")
        @DecimalMax("50.00")
        BigDecimal maxManualDeviationPercent,

        /* How long an exchange quote keeps its price. */
        @NotNull
        Duration quoteTtl
) {
}
