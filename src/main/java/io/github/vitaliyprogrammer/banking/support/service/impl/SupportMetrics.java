package io.github.vitaliyprogrammer.banking.support.service.impl;

import io.github.vitaliyprogrammer.banking.common.metrics.AfterCommit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Support tickets opened and resolved, counted after commit. */
@Component
@RequiredArgsConstructor
public class SupportMetrics {

    private final MeterRegistry registry;

    public void ticketOpened() {

        AfterCommit.run(() -> tickets("opened").increment());
    }

    public void ticketResolved() {

        AfterCommit.run(() -> tickets("resolved").increment());
    }

    private Counter tickets(String event) {

        return Counter.builder("bank.support.tickets")
                .description("Support tickets opened and resolved")
                .tag("event", event)
                .register(registry);
    }
}
