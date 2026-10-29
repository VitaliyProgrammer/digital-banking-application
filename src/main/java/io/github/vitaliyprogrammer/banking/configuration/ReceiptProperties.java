package io.github.vitaliyprogrammer.banking.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.receipts.*". */
@Validated
@ConfigurationProperties(prefix = "banking.receipts")
public record ReceiptProperties(
        /* Sender of receipt emails, e.g. "Digital Bank <no-reply@digital-bank.local>". */
        @NotBlank
        String mailFrom
) {
}
