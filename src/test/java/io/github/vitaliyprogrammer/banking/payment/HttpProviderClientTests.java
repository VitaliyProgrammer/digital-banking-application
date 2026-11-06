package io.github.vitaliyprogrammer.banking.payment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.payment.configuration.ProviderProperties;
import io.github.vitaliyprogrammer.banking.payment.dto.request.ProviderPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.entity.status.ProviderOutcome;
import io.github.vitaliyprogrammer.banking.payment.service.impl.HttpProviderClient;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * How every way a provider can fail is read. The line that matters: "nothing was sent" may be retried, while
 * "sent, answer unclear" must go to reconciliation, because the provider may already have taken the money.
 * A real HTTP server (WireMock) breaks the connection the way a network does; no Spring, so it runs in seconds.
 */
class HttpProviderClientTests {

    private static final WireMockServer provider = new WireMockServer(wireMockConfig().dynamicPort());

    private static final ProviderPaymentRequestDto PAYMENT = new ProviderPaymentRequestDto(
            UUID.randomUUID(), "4410012345", new BigDecimal("300.00"), Currency.UAH);

    private HttpProviderClient client;

    @BeforeAll
    static void startProvider() {
        provider.start();
    }

    @AfterAll
    static void stopProvider() {
        provider.stop();
    }

    @BeforeEach
    void setUp() {
        provider.resetAll();
        client = clientFor(provider.baseUrl());
    }

    @Test
    void clearAnswersAreTakenAsTheyAre() {
        provider.stubFor(post(urlPathMatching("/ELECTRO/payments")).willReturn(okJson("{\"status\": \"ACCEPTED\"}")));
        assertThat(client.send("ELECTRO", PAYMENT).outcome()).isEqualTo(ProviderOutcome.ACCEPTED);

        provider.stubFor(post(urlPathMatching("/ELECTRO/payments")).willReturn(aResponse().withStatus(422)));
        assertThat(client.send("ELECTRO", PAYMENT).outcome()).isEqualTo(ProviderOutcome.REJECTED);
    }

    /** The connection breaks before a readable answer: the request may have arrived, so the outcome is unknown. */
    @ParameterizedTest
    @EnumSource(value = Fault.class, names = {"MALFORMED_RESPONSE_CHUNK", "RANDOM_DATA_THEN_CLOSE",
            "CONNECTION_RESET_BY_PEER", "EMPTY_RESPONSE"})
    void connectionBrokenBeforeTheAnswerIsUnclear(Fault fault) {
        provider.stubFor(post(urlPathMatching("/ELECTRO/payments")).willReturn(aResponse().withFault(fault)));

        assertThat(client.send("ELECTRO", PAYMENT).outcome()).isEqualTo(ProviderOutcome.NO_ANSWER);
    }

    /**
     * The case that used to escape as an exception: "200 OK" arrives, but the body cannot be read (cut off, or half a
     * JSON). Spring reports it as a plain RestClientException, which no catch took: it stopped the whole batch, and
     * the payment never reached reconciliation although the provider may have taken the money.
     */
    @Test
    void unreadableAnswerIsUnclear() {
        provider.stubFor(post(urlPathMatching("/ELECTRO/payments")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("{\"status\": \"ACC")));

        assertThat(client.send("ELECTRO", PAYMENT).outcome()).isEqualTo(ProviderOutcome.NO_ANSWER);
    }

    @Test
    void brokenStatusCheckIsUnclearToo() {
        provider.stubFor(get(urlPathMatching("/ELECTRO/payments/.+")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("{\"status\": \"ACC")));

        assertThat(client.checkStatus("ELECTRO", PAYMENT.reference()).outcome()).isEqualTo(ProviderOutcome.NO_ANSWER);
    }

    @Test
    void providerErrorIsUnclear() {
        provider.stubFor(post(urlPathMatching("/ELECTRO/payments")).willReturn(aResponse().withStatus(503)));

        assertThat(client.send("ELECTRO", PAYMENT).outcome()).isEqualTo(ProviderOutcome.NO_ANSWER);
    }

    @Test
    void nobodyListeningMeansNothingWasSent() {
        // Port 1: nothing listens there, so the connection is refused at once.
        HttpProviderClient unreachable = clientFor("http://localhost:1");

        assertThat(unreachable.send("ELECTRO", PAYMENT).outcome()).isEqualTo(ProviderOutcome.NOT_DELIVERED);
    }

    private static HttpProviderClient clientFor(String baseUrl) {
        return new HttpProviderClient(new ProviderProperties(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(2), false));
    }
}
