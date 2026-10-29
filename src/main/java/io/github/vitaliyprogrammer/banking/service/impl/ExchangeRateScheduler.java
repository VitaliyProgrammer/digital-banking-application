package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.exception.OfficialRatesUnavailableException;
import io.github.vitaliyprogrammer.banking.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Syncs once on startup (an empty database gets rates right away) and then on the cron schedule.
 * The NBU changes its rate once a day, but syncing every hour is a free retry: a known day is skipped,
 * and a missed morning is caught up an hour later. Several instances may run it at once for the same reason.
 * If the NBU is down nothing breaks: the last rate stays and the rates table marks it as stale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.exchange-rates.sync-enabled", havingValue = "true")
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {

        sync();
    }

    @Scheduled(cron = "${banking.exchange-rates.sync-cron}", zone = "Europe/Kyiv")
    public void syncOnSchedule() {

        sync();
    }

    private void sync() {

        try {
            exchangeRateService.syncOfficialRates();
        } catch (OfficialRatesUnavailableException e) {
            log.warn("{}. The last known rates stay in use and are shown as stale", e.getMessage());
        }
    }
}
