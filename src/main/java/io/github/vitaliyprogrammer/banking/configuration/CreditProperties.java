package io.github.vitaliyprogrammer.banking.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.credit.*": the numbers behind every credit decision, kept out of the code. */
@Validated
@ConfigurationProperties(prefix = "banking.credit")
public record CreditProperties(
        /* A client must have had a UAH current account at least this long. */
        @Min(0)
        int minAccountAgeDays,

        /* Income is counted over this many days before the application. */
        @Min(30)
        int incomeWindowDays,

        /* Less income than this in the window: the client cannot be scored, the answer is no. */
        @NotNull
        @DecimalMin("0.00")
        BigDecimal minIncome,

        /* The affordable limit is this share of the average monthly income, e.g. 0.50. */
        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("1.00")
        BigDecimal incomeShare,

        /* Up to this amount (and within the affordable limit) the rules approve on their own. */
        @NotNull
        BigDecimal autoApproveUpTo,

        /* A request above affordable × this factor is declined; between the two it goes to a person. */
        @NotNull
        @DecimalMin("1.00")
        BigDecimal reviewFactor,

        /* No limit is ever larger than this, whoever approves it. */
        @NotNull
        BigDecimal maxLimit
) {
}
