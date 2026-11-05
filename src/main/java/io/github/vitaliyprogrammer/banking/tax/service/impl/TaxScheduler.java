package io.github.vitaliyprogrammer.banking.tax.service.impl;

import io.github.vitaliyprogrammer.banking.tax.service.TaxService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every morning: reminders before due dates, and auto-pay on them. Running it twice in a day changes nothing. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.tax.jobs-enabled", havingValue = "true")
public class TaxScheduler {

    private final TaxService taxService;

    private final Clock clock;

    @Scheduled(cron = "${banking.tax.daily-cron}", zone = "Europe/Kyiv")
    public void runDaily() {

        taxService.runDaily(LocalDate.now(clock.withZone(TaxPeriod.KYIV)));
    }
}
