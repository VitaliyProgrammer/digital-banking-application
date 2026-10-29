package io.github.vitaliyprogrammer.banking.cashback;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CashbackResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.CashbackService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import io.github.vitaliyprogrammer.banking.service.impl.BillPaymentProcessor;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proof tests for phase 7.2. Cashback is earned only for bill payments that really completed, never above
 * the monthly cap even when payments complete at the same moment, and paid once a month.
 * Rates in the configuration: mobile 5%, internet 3%, utilities 1%; cap 500 UAH a month.
 */
class CashbackIntegrationTests extends IntegrationTest {

    @RegisterExtension
    static WireMockExtension provider = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void pointBankToFakeProvider(DynamicPropertyRegistry registry) {
        registry.add("banking.providers.base-url", provider::baseUrl);
    }

    @Autowired
    private CashbackService cashbackService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private BillPaymentProcessor billPaymentProcessor;

    private final YearMonth thisMonth = YearMonth.now(ZoneId.of("Europe/Kyiv"));

    private UUID clientId;

    private UUID account;

    @BeforeEach
    void clientWithMoney() {
        clientId = createUser();
        account = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(clientId, UUID.randomUUID().toString(),
                new TopUpRequestDto(account, new BigDecimal("50000.00")));
    }

    @Test
    void cashbackDependsOnTheCategoryAndComesOnlyAfterTheProviderAccepted() {
        payBill("mobile-one", "+380671234567", "200.00");
        payBill("city-power", "4410012345", "1000.00");

        // Held, not yet accepted by the provider: nothing earned.
        assertThat(cashbackService.getCashback(clientId, thisMonth).earned()).isEqualByComparingTo("0.00");

        acceptAndProcess();

        CashbackResponseDto cashback = cashbackService.getCashback(clientId, thisMonth);
        assertThat(cashback.earned()).isEqualByComparingTo("20.00");        // 5% of 200 + 1% of 1000
        assertThat(cashback.remaining()).isEqualByComparingTo("480.00");
        assertThat(cashback.items()).hasSize(2);
    }

    @Test
    void theMonthlyCapHoldsEvenWhenPaymentsCompleteAtTheSameMoment() throws Exception {
        // Four mobile top-ups of 3000 would earn 4 × 150 = 600, but the cap is 500.
        for (int i = 0; i < 4; i++) {
            payBill("mobile-one", "+38067000000" + i, "3000.00");
        }
        provider.stubFor(post(urlPathMatching("/.+/payments")).willReturn(okJson("{\"status\": \"ACCEPTED\"}")));

        runConcurrently(4, i -> () -> billPaymentProcessor.processPending());

        assertThat(cashbackService.getCashback(clientId, thisMonth).earned()).isEqualByComparingTo("500.00");
    }

    @Test
    void cashbackIsPaidOnceAMonthHoweverOftenThePayoutRuns() {
        payBill("fiber-net", "NET-1001", "400.00");
        acceptAndProcess();
        BigDecimal before = balanceOf(account);

        cashbackService.payCashback(thisMonth);
        cashbackService.payCashback(thisMonth);

        assertThat(balanceOf(account)).isEqualByComparingTo(before.add(new BigDecimal("12.00")));   // 3% of 400
        assertThat(cashbackService.getCashback(clientId, thisMonth).paidOut()).isTrue();
    }

    private void payBill(String providerCode, String accountNumber, String amount) {
        UUID providerId = jdbcTemplate.queryForObject("SELECT id FROM providers WHERE code = ?", UUID.class,
                providerCode);
        paymentService.payBill(clientId, UUID.randomUUID().toString(),
                new BillPaymentRequestDto(providerId, accountNumber, account, new BigDecimal(amount)));
    }

    private void acceptAndProcess() {
        provider.stubFor(post(urlPathMatching("/.+/payments")).willReturn(okJson("{\"status\": \"ACCEPTED\"}")));
        billPaymentProcessor.processPending();
    }
}
