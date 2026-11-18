package io.github.vitaliyprogrammer.banking.payment;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.SavingsTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.SpendingCategory;
import io.github.vitaliyprogrammer.banking.payment.dto.response.SpendingResponseDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.SpendingResponseDto.CategoryTotal;
import io.github.vitaliyprogrammer.banking.payment.exception.InvalidPeriodException;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.payment.service.impl.BillPaymentProcessor;
import io.github.vitaliyprogrammer.banking.savings.service.SavingsService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Proof test for the home screen's "where the money went": only real spending counts, each in its category. */
class SpendingIntegrationTests extends IntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private SavingsService savingsService;

    @Autowired
    private BillPaymentProcessor billPaymentProcessor;

    @Autowired
    private Clock clock;

    @Test
    void spendingCountsTransfersToPeopleSavingsAndBillsButNotTopUps() {
        UUID clientId = createUser();
        UUID clientAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(clientId, newKey(), new TopUpRequestDto(clientAccount, new BigDecimal("10000.00")));

        UUID friendId = createUser();
        String friendAccount = accountService.open(friendId, new OpenAccountRequestDto(Currency.UAH)).number();
        paymentService.transfer(clientId, newKey(),
                new TransferRequestDto(clientAccount, friendAccount, new BigDecimal("1000.00"), null));

        UUID jar = savingsService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.depositToSavings(clientId, newKey(), jar,
                new SavingsTransferRequestDto(clientAccount, new BigDecimal("500.00")));

        wireMock.stubFor(post(urlPathEqualTo("/city-power/payments")).willReturn(okJson("{\"status\": \"ACCEPTED\"}")));
        UUID electricity = jdbcTemplate.queryForObject("SELECT id FROM providers WHERE code = 'city-power'", UUID.class);
        paymentService.payBill(clientId, newKey(),
                new BillPaymentRequestDto(electricity, "4410012345", clientAccount, new BigDecimal("300.00")));
        billPaymentProcessor.processPending();

        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Europe/Kyiv")));
        SpendingResponseDto spending = paymentService.getSpending(clientId, today.withDayOfMonth(1), today);

        assertThat(spending.categories()).containsExactly(
                new CategoryTotal(SpendingCategory.TRANSFERS, new BigDecimal("1000.00")),
                new CategoryTotal(SpendingCategory.SAVINGS, new BigDecimal("500.00")),
                new CategoryTotal(SpendingCategory.UTILITIES, new BigDecimal("300.00")));
        assertThat(spending.total()).isEqualByComparingTo("1800.00");

        SpendingResponseDto yesterday = paymentService.getSpending(clientId, today.minusDays(1), today.minusDays(1));
        assertThat(yesterday.categories()).isEmpty();
        assertThat(yesterday.total()).isEqualByComparingTo("0.00");
    }

    @Test
    void aPeriodLongerThanAYearIsRefused() {
        LocalDate from = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> paymentService.getSpending(UUID.randomUUID(), from, from.plusDays(366)))
                .isInstanceOf(InvalidPeriodException.class);
        assertThatThrownBy(() -> paymentService.getSpending(UUID.randomUUID(), from, from.minusDays(1)))
                .isInstanceOf(InvalidPeriodException.class);
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
