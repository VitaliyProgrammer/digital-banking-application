package io.github.vitaliyprogrammer.banking.business.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from "banking.business.*". The fixed amounts of groups 1 and 2 follow the subsistence minimum and the
 * minimum wage, which change every year, so they live in the configuration, not in the code.
 */
@Validated
@ConfigurationProperties(prefix = "banking.business")
public record BusinessProperties(
        /* Group 1: a fixed tax every month. */
        @NotNull
        @DecimalMin("0.00")
        BigDecimal group1MonthlyTax,

        /* Group 2: a fixed tax every month. */
        @NotNull
        @DecimalMin("0.00")
        BigDecimal group2MonthlyTax,

        /* Group 3: percent of every income. */
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal group3RatePercent,

        /* Group 3 for a VAT payer: percent of every income. */
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal group3VatRatePercent
) {
}
