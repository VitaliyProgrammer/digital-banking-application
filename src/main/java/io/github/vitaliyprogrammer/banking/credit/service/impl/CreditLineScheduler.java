package io.github.vitaliyprogrammer.banking.credit.service.impl;

import io.github.vitaliyprogrammer.banking.credit.service.CreditLineService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safe to repeat and to run on several instances: a month gets one statement thanks to a unique key,
 * and a statement is settled once under a row lock. A run that was missed settles everything overdue next time.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.credit-line.jobs-enabled", havingValue = "true")
public class CreditLineScheduler {

    private final CreditLineService creditLineService;

    private final Clock clock;

    @Scheduled(cron = "${banking.credit-line.statement-cron}", zone = "Europe/Kyiv")
    public void closeMonth() {

        creditLineService.closeMonth(LocalDate.now(clock.withZone(GracePeriod.KYIV)));
    }
}
