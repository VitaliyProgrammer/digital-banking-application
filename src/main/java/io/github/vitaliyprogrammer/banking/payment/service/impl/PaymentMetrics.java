package io.github.vitaliyprogrammer.banking.payment.service.impl;

import io.github.vitaliyprogrammer.banking.common.metrics.AfterCommit;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Payments that reached a final state, counted after commit. Tags hold only small, fixed sets of values:
 * a user id as a tag would create a time series per client and overload Prometheus.
 */
@Component
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry registry;

    public void paymentCompleted(PaymentType type) {

        AfterCommit.run(() -> payments(type, "completed").increment());
    }

    /** A bill payment the provider rejected or never received: the money came back. */
    public void paymentFailed(PaymentType type) {

        AfterCommit.run(() -> payments(type, "failed").increment());
    }

    private Counter payments(PaymentType type, String result) {

        return Counter.builder("bank.payments")
                .description("Payments that reached a final state")
                .tag("type", type.name().toLowerCase(Locale.ROOT))
                .tag("result", result)
                .register(registry);
    }
}
