package io.github.vitaliyprogrammer.banking.credit.service.impl;

import io.github.vitaliyprogrammer.banking.common.metrics.AfterCommit;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.credit.entity.status.DecisionSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Credit applications and decisions, counted after commit. */
@Component
@RequiredArgsConstructor
public class CreditMetrics {

    private final MeterRegistry registry;

    /** Every application, by what the rules said: APPROVED, DECLINED or MANUAL_REVIEW. */
    public void creditApplicationScored(CreditApplicationStatus outcome) {

        AfterCommit.run(() -> Counter.builder("bank.credit.applications")
                .description("Credit applications by the outcome of automatic scoring")
                .tag("outcome", tag(outcome))
                .register(registry)
                .increment());
    }

    /** A final decision: by the rules (AUTO) or by a person (MANUAL). */
    public void creditDecided(CreditApplicationStatus status, DecisionSource source) {

        AfterCommit.run(() -> Counter.builder("bank.credit.decisions")
                .description("Final credit decisions")
                .tag("status", tag(status))
                .tag("source", tag(source))
                .register(registry)
                .increment());
    }

    private static String tag(Enum<?> value) {

        return value.name().toLowerCase(Locale.ROOT);
    }
}
