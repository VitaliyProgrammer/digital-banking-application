package io.github.vitaliyprogrammer.banking.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.exchange.dto.request.ExchangeQuoteRequestDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.ExchangeQuoteResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.exception.ExchangeQuoteAlreadyUsedException;
import io.github.vitaliyprogrammer.banking.exchange.exception.ExchangeQuoteExpiredException;
import io.github.vitaliyprogrammer.banking.exchange.exception.ExchangeQuoteNotFoundException;
import io.github.vitaliyprogrammer.banking.exchange.exception.SameCurrencyExchangeException;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeQuoteService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.ExchangeRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 5.2. The client gets exactly the quoted amount, a quote is spent at most once,
 * and after every exchange each currency still balances to zero (checked by {@link IntegrationTest}).
 */
class CurrencyExchangeIntegrationTests extends IntegrationTest {

    @Autowired
    private ExchangeQuoteService exchangeQuoteService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    private UUID clientId;

    private UUID uahAccount;

    private UUID usdAccount;

    private UUID eurAccount;

    @BeforeEach
    void clientWithThreeAccountsAndFixedRates() {
        // Round numbers make the expected amounts easy to check by hand.
        jdbcTemplate.update("DELETE FROM bank_rates");
        setBankRate(Currency.USD, "40.0000", "41.0000");
        setBankRate(Currency.EUR, "45.0000", "46.0000");

        clientId = createUser();
        uahAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        usdAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.USD)).id();
        eurAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.EUR)).id();
        topUp(uahAccount, "10000.00");
        topUp(usdAccount, "500.00");
    }

    @Test
    void sellingDollarsGivesExactlyTheQuotedHryvnias() {
        ExchangeQuoteResponseDto quote = quote(usdAccount, uahAccount, "100.00");
        assertThat(quote.buyAmount()).isEqualByComparingTo("4000.00");

        PaymentResponseDto payment = exchange(quote.id());

        assertThat(payment.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.creditedAmount()).isEqualByComparingTo("4000.00");
        assertThat(balanceOf(usdAccount)).isEqualByComparingTo("400.00");
        assertThat(balanceOf(uahAccount)).isEqualByComparingTo("14000.00");
    }

    @Test
    void buyingDollarsRoundsDownInTheBanksFavour() {
        // 1000 / 41 = 24.390243...: the client gets 24.39, never 24.40.
        ExchangeQuoteResponseDto quote = quote(uahAccount, usdAccount, "1000.00");

        assertThat(quote.buyAmount()).isEqualByComparingTo("24.39");
    }

    @Test
    void dollarsToEurosGoThroughHryvnia() {
        // 100 USD × 40 = 4000 UAH; 4000 / 46 = 86.956...: 86.95 EUR.
        exchange(quote(usdAccount, eurAccount, "100.00").id());

        assertThat(balanceOf(eurAccount)).isEqualByComparingTo("86.95");
    }

    @Test
    void theQuotedPriceHoldsEvenIfTheRateChangesBeforeTheClientAccepts() {
        ExchangeQuoteResponseDto quote = quote(usdAccount, uahAccount, "100.00");

        setBankRate(Currency.USD, "30.0000", "31.0000");
        exchange(quote.id());

        assertThat(balanceOf(uahAccount)).isEqualByComparingTo("14000.00");
    }

    @Test
    void anExpiredQuoteMovesNoMoney() {
        ExchangeQuoteResponseDto quote = quote(usdAccount, uahAccount, "100.00");
        // Instead of waiting 30 seconds, move the expiry into the past.
        jdbcTemplate.update("""
                UPDATE exchange_quotes SET created_at = now() - interval '1 minute',
                                           expires_at = now() - interval '1 second'
                WHERE id = ?
                """, quote.id());

        assertThatThrownBy(() -> exchange(quote.id())).isInstanceOf(ExchangeQuoteExpiredException.class);
        assertThat(balanceOf(usdAccount)).isEqualByComparingTo("500.00");
    }

    @Test
    void aQuoteIsSpentOnceEvenByFiveParallelRequestsWithDifferentKeys() throws Exception {
        ExchangeQuoteResponseDto quote = quote(usdAccount, uahAccount, "100.00");

        List<String> results = runConcurrently(5, i -> () -> {
            try {
                exchange(quote.id());
                return "exchanged";
            } catch (ExchangeQuoteAlreadyUsedException e) {
                return "already used";
            }
        });

        assertThat(results).containsOnlyOnce("exchanged").containsOnly("exchanged", "already used");
        assertThat(balanceOf(usdAccount)).isEqualByComparingTo("400.00");
    }

    @Test
    void nobodyElseCanSpendMyQuote() {
        ExchangeQuoteResponseDto quote = quote(usdAccount, uahAccount, "100.00");
        UUID stranger = createUser();

        assertThatThrownBy(() -> paymentService.exchange(stranger, UUID.randomUUID().toString(),
                new ExchangeRequestDto(quote.id())))
                .isInstanceOf(ExchangeQuoteNotFoundException.class);
    }

    @Test
    void sameCurrencyIsNotAnExchange() {
        assertThatThrownBy(() -> quote(uahAccount, uahAccount, "10.00"))
                .isInstanceOf(SameCurrencyExchangeException.class);
    }

    private ExchangeQuoteResponseDto quote(UUID from, UUID to, String amount) {
        return exchangeQuoteService.createQuote(clientId, new ExchangeQuoteRequestDto(from, to, new BigDecimal(amount)));
    }

    private PaymentResponseDto exchange(UUID quoteId) {
        return paymentService.exchange(clientId, UUID.randomUUID().toString(), new ExchangeRequestDto(quoteId));
    }

    private void topUp(UUID accountId, String amount) {
        paymentService.topUp(clientId, UUID.randomUUID().toString(), new TopUpRequestDto(accountId, new BigDecimal(amount)));
    }

    private void setBankRate(Currency currency, String buy, String sell) {
        jdbcTemplate.update("""
                INSERT INTO bank_rates (id, currency, buy, sell, mode, official_rate, valid_from)
                VALUES (gen_random_uuid(), ?, ?, ?, 'MANUAL', ?, clock_timestamp())
                """, currency.name(), new BigDecimal(buy), new BigDecimal(sell), new BigDecimal(buy));
    }
}
