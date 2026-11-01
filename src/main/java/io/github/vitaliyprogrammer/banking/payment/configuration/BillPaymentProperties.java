package io.github.vitaliyprogrammer.banking.payment.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Bound from "banking.bill-payments.*". */
@Validated
@ConfigurationProperties(prefix = "banking.bill-payments")
public record BillPaymentProperties(
        /* Off in tests, which drive the worker by hand to control timing. */
        boolean workerEnabled,

        @NotNull
        Duration processingDelay,

        @NotNull
        Duration reconciliationDelay,

        /* A claimed payment is invisible to other workers this long; if its worker dies, it is retried after that. */
        @NotNull
        Duration lease,

        @Min(1)
        int batchSize,

        /* After this many failed deliveries (provider unreachable) the payment fails and the money comes back. */
        @Min(1)
        int maxDeliveryAttempts
) {
}
