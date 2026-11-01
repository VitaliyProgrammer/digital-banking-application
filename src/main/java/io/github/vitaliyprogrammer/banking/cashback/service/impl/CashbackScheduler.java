package io.github.vitaliyprogrammer.banking.cashback.service.impl;

import io.github.vitaliyprogrammer.banking.cashback.service.CashbackService;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Pays last month's cashback on the 1st. Safe to repeat and to run on several instances (unique payout rows). */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.cashback.jobs-enabled", havingValue = "true")
public class CashbackScheduler {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private final CashbackService cashbackService;

    private final Clock clock;

    @Scheduled(cron = "${banking.cashback.payout-cron}", zone = "Europe/Kyiv")
    public void payCashback() {

        cashbackService.payCashback(YearMonth.now(clock.withZone(KYIV)).minusMonths(1));
    }
}
