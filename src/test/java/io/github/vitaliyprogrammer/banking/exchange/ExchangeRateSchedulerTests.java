package io.github.vitaliyprogrammer.banking.exchange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.vitaliyprogrammer.banking.exchange.exception.OfficialRatesUnavailableException;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeRateService;
import io.github.vitaliyprogrammer.banking.exchange.service.impl.ExchangeRateScheduler;
import org.junit.jupiter.api.Test;

/** A fresh bank must not wait an hour for its first rates when the very first request to the NBU fails. */
class ExchangeRateSchedulerTests {

    private final ExchangeRateService exchangeRateService = mock(ExchangeRateService.class);

    private final ExchangeRateScheduler scheduler = new ExchangeRateScheduler(exchangeRateService);

    @Test
    void aFailedStartupSyncIsRetriedUntilItWorksAndThenNoMore() {
        when(exchangeRateService.syncOfficialRates())
                .thenThrow(new OfficialRatesUnavailableException("NBU rates are unavailable"))
                .thenReturn(null);

        scheduler.syncOnStartup();          // fails: the network is not up yet
        scheduler.retryUntilFirstSync();    // works
        scheduler.retryUntilFirstSync();    // nothing to do any more
        scheduler.retryUntilFirstSync();

        verify(exchangeRateService, times(2)).syncOfficialRates();
    }

    @Test
    void afterASuccessfulStartupSyncTheRetryDoesNothing() {
        when(exchangeRateService.syncOfficialRates()).thenReturn(null);

        scheduler.syncOnStartup();
        scheduler.retryUntilFirstSync();

        verify(exchangeRateService, times(1)).syncOfficialRates();
    }
}
