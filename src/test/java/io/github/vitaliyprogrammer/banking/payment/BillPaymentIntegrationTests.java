package io.github.vitaliyprogrammer.banking.payment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.payment.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.payment.service.impl.BillPaymentProcessor;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 4. The provider is a real HTTP server (WireMock) that can be slow, refuse or accept.
 * Whatever it does, the client's money ends up either with the provider or back on the client's account,
 * never stuck or lost, and the ledger reconciles after every test.
 */
class BillPaymentIntegrationTests extends IntegrationTest {

    private static final String PROVIDER = "city-power";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private BillPaymentProcessor billPaymentProcessor;

    private UUID clientId;

    private UUID clientAccount;

    @BeforeEach
    void clientWithMoney() {
        clientId = createUser();
        clientAccount = accountService.open(clientId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(clientId, newKey(), new TopUpRequestDto(clientAccount, new BigDecimal("1000.00")));
    }

    @Test
    void acceptedPaymentIsSentOnceEvenWithFiveWorkers() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/" + PROVIDER + "/payments"))
                .willReturn(okJson("{\"status\": \"ACCEPTED\"}").withFixedDelay(200)));

        BigDecimal inTransitBefore = inTransit();
        PaymentResponseDto payment = payElectricity("300.00");

        assertThat(payment.status()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.fee()).as("0.5% utilities fee").isEqualByComparingTo("1.50");
        // 300.00 plus the 0.5% utilities fee (1.50) are held at once; a rejection returns both.
        assertThat(balanceOf(clientAccount)).as("held right away").isEqualByComparingTo("698.50");

        // Five application instances wake up at the same moment and look for work.
        runConcurrently(5, i -> () -> billPaymentProcessor.processPending());

        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/" + PROVIDER + "/payments")));
        assertThat(statusOf(payment.id())).isEqualTo(PaymentStatus.COMPLETED);
        // HOLD is shared by all tests, so compare with what was in transit before this payment.
        assertThat(inTransit()).as("nothing left in transit").isEqualByComparingTo(inTransitBefore);
    }

    @Test
    void rejectedPaymentReturnsTheMoney() {
        wireMock.stubFor(post(urlPathEqualTo("/" + PROVIDER + "/payments"))
                .willReturn(okJson("{\"status\": \"REJECTED\", \"reason\": \"Unknown personal account\"}")));

        PaymentResponseDto payment = payElectricity("300.00");
        billPaymentProcessor.processPending();

        PaymentResponseDto result = paymentService.getPayment(clientId, payment.id());
        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("Unknown personal account");
        assertThat(balanceOf(clientAccount)).isEqualByComparingTo("1000.00");
    }

    @Test
    void timeoutBecomesUnknownAndReconciliationFinishesTheJob() {
        // The provider takes the money, but answers after our 1.5 s timeout.
        wireMock.stubFor(post(urlPathEqualTo("/" + PROVIDER + "/payments"))
                .willReturn(okJson("{\"status\": \"ACCEPTED\"}").withFixedDelay(4_000)));

        PaymentResponseDto payment = payElectricity("300.00");
        billPaymentProcessor.processPending();

        // The dangerous moment: we do not know what happened. Not FAILED, so the money is NOT returned yet.
        assertThat(statusOf(payment.id())).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(balanceOf(clientAccount)).isEqualByComparingTo("698.50");

        wireMock.stubFor(get(urlPathMatching("/" + PROVIDER + "/payments/.+"))
                .willReturn(okJson("{\"status\": \"ACCEPTED\"}")));
        makeDueNow(payment.id());
        billPaymentProcessor.reconcileUnknown();

        assertThat(statusOf(payment.id())).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(balanceOf(clientAccount)).isEqualByComparingTo("698.50");
        // Reconciliation asked; it did not pay a second time.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/" + PROVIDER + "/payments")));
    }

    @Test
    void unknownPaymentThatTheProviderNeverGotIsReturned() {
        wireMock.stubFor(post(urlPathEqualTo("/" + PROVIDER + "/payments"))
                .willReturn(aResponse().withStatus(503)));

        PaymentResponseDto payment = payElectricity("300.00");
        billPaymentProcessor.processPending();
        assertThat(statusOf(payment.id())).isEqualTo(PaymentStatus.UNKNOWN);

        wireMock.stubFor(get(urlPathMatching("/" + PROVIDER + "/payments/.+"))
                .willReturn(aResponse().withStatus(404)));
        makeDueNow(payment.id());
        billPaymentProcessor.reconcileUnknown();

        assertThat(statusOf(payment.id())).isEqualTo(PaymentStatus.FAILED);
        assertThat(balanceOf(clientAccount)).isEqualByComparingTo("1000.00");
    }

    private PaymentResponseDto payElectricity(String amount) {
        UUID providerId = jdbcTemplate.queryForObject(
                "SELECT id FROM providers WHERE code = ?", UUID.class, PROVIDER);
        return paymentService.payBill(clientId, newKey(),
                new BillPaymentRequestDto(providerId, "4410012345", clientAccount, new BigDecimal(amount)));
    }

    private PaymentStatus statusOf(UUID paymentId) {
        return paymentService.getPayment(clientId, paymentId).status();
    }

    /** Money in transit: HOLD is split into shards (V22), and a hold and its capture may use different ones. */
    private BigDecimal inTransit() {
        return jdbcTemplate.queryForObject(
                "SELECT sum(balance) FROM accounts WHERE type = 'HOLD' AND currency = 'UAH'", BigDecimal.class);
    }

    /** Instead of sleeping until the next reconciliation is due, move that moment into the past. */
    private void makeDueNow(UUID paymentId) {
        jdbcTemplate.update("UPDATE payments SET next_attempt_at = now() - interval '1 second' WHERE id = ?", paymentId);
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
