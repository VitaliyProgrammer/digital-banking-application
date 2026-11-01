package io.github.vitaliyprogrammer.banking.credit.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.credit-line.*": the price of a credit line once its interest-free period is missed. */
@Validated
@ConfigurationProperties(prefix = "banking.credit-line")
public record CreditLineProperties(
        /* Off in tests, which close the month themselves for the day they choose. */
        boolean jobsEnabled,

        @NotBlank
        String statementCron,

        /* Interest per month in percent on what is left of a statement after its due date, e.g. 2.50. */
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("10.00")
        BigDecimal monthlyInterestRate,

        /* The minimum payment is this share of the statement in percent, e.g. 5.00 ... */
        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("100.00")
        BigDecimal minimumPaymentPercent,

        /* ... but never less than this amount (or the whole debt, if it is smaller). */
        @NotNull
        @DecimalMin("0.01")
        BigDecimal minimumPaymentFloor,

        /* Charged once for a statement whose minimum payment was not made by the due date. */
        @NotNull
        @DecimalMin("0.00")
        BigDecimal lateFee
) {
}
