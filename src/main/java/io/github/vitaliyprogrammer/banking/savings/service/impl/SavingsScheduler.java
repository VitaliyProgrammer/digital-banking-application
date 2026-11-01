package io.github.vitaliyprogrammer.banking.savings.service.impl;

import io.github.vitaliyprogrammer.banking.savings.service.SavingsService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Both jobs are safe to repeat and to run on several instances: a day and a month are processed once
 * thanks to unique keys in the database, so no distributed lock is needed here either.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.savings.jobs-enabled", havingValue = "true")
public class SavingsScheduler {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    /** If the application was down for a few days, the next run catches those days up. */
    private static final int CATCH_UP_DAYS = 7;

    private final SavingsService savingsService;

    private final Clock clock;

    @Scheduled(cron = "${banking.savings.accrual-cron}", zone = "Europe/Kyiv")
    public void accrueInterest() {

        LocalDate today = LocalDate.now(clock.withZone(KYIV));

        for (int daysAgo = CATCH_UP_DAYS; daysAgo >= 1; daysAgo--) {
            savingsService.accrueInterest(today.minusDays(daysAgo));
        }
    }

    @Scheduled(cron = "${banking.savings.payout-cron}", zone = "Europe/Kyiv")
    public void payInterest() {

        savingsService.payInterest(YearMonth.now(clock.withZone(KYIV)).minusMonths(1));
    }
}
