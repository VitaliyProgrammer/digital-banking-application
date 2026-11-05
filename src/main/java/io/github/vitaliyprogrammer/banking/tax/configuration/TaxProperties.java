package io.github.vitaliyprogrammer.banking.tax.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.tax.*". */
@Validated
@ConfigurationProperties(prefix = "banking.tax")
public record TaxProperties(
        /* How many days before the due date the client gets a reminder. */
        @Min(0)
        @Max(30)
        int reminderDaysBefore
) {
}
