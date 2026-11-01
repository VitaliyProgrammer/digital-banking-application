package io.github.vitaliyprogrammer.banking.exchange;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.exchange.exception.OfficialRatesUnavailableException;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeRateService;
import io.github.vitaliyprogrammer.banking.identity.entity.status.Role;
import io.github.vitaliyprogrammer.banking.identity.repository.UserRepository;
import io.github.vitaliyprogrammer.banking.identity.service.AccessTokenService;
import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Proof tests for phase 5.1. The NBU is a WireMock server that can answer, repeat itself or be down.
 * The rates table always shows something sensible, and only an admin can change the bank's rates.
 */
class ExchangeRateIntegrationTests extends IntegrationTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenService accessTokenService;

    private final LocalDate today = LocalDate.now(KYIV);

    @BeforeEach
    void noRatesYet() {
        // Only this class uses the rate tables, so every test starts from an empty rates history.
        jdbcTemplate.update("DELETE FROM bank_rates");
        jdbcTemplate.update("DELETE FROM official_rates");
    }

    @Test
    void syncStoresTodaysRatesOnceAndPricesThemWithTheDefaultSpread() throws Exception {
        nbuAnswers(today, "41.4729", "48.1234");

        assertThat(exchangeRateService.syncOfficialRates().savedRates()).isEqualTo(2);
        // Hourly re-runs and a second instance see the same day and store nothing.
        assertThat(exchangeRateService.syncOfficialRates().savedRates()).isZero();

        // Public: no token. 1.5% default spread, rounded in the bank's favour: buy down, sell up.
        mockMvc.perform(MockMvcRequestBuilders.get("/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].officialRate").value(41.4729))
                .andExpect(jsonPath("$[0].stale").value(false))
                .andExpect(jsonPath("$[0].buy").value(40.8508))
                .andExpect(jsonPath("$[0].sell").value(42.0950))
                .andExpect(jsonPath("$[0].mode").value("SPREAD"));
    }

    @Test
    void whenTheNbuIsDownTheLastRateStaysAndIsMarkedStale() throws Exception {
        nbuAnswers(today.minusDays(1), "41.0000", "48.0000");
        exchangeRateService.syncOfficialRates();

        wireMock.stubFor(get(urlPathEqualTo(NBU_PATH)).willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> exchangeRateService.syncOfficialRates())
                .isInstanceOf(OfficialRatesUnavailableException.class);

        mockMvc.perform(MockMvcRequestBuilders.get("/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].officialRate").value(41.0))
                .andExpect(jsonPath("$[0].officialRateDate").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$[0].stale").value(true));
    }

    @Test
    void onlyAnAdminSetsRatesAndEveryChangeKeepsItsAuthor() throws Exception {
        nbuAnswers(today, "41.5000", "48.0000");
        exchangeRateService.syncOfficialRates();
        String manualRate = """
                {"mode": "MANUAL", "buy": 41.10, "sell": 41.95, "comment": "Morning rate"}
                """;

        setUsdRate(null, manualRate).andExpect(status().isUnauthorized());
        setUsdRate(tokenOf(createUser()), manualRate).andExpect(status().isForbidden());

        String adminEmail = "admin-" + UUID.randomUUID() + "@bank.test";
        String adminToken = tokenOf(createAdmin(adminEmail));
        setUsdRate(adminToken, manualRate)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buy").value(41.1))
                .andExpect(jsonPath("$.sell").value(41.95))
                .andExpect(jsonPath("$.mode").value("MANUAL"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/exchange-rates/USD/changes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].setByEmail").value(adminEmail))
                .andExpect(jsonPath("$.content[0].comment").value("Morning rate"))
                // The older entry is the system's default spread: no author.
                .andExpect(jsonPath("$.content[1].setBy").value(nullValue()));
    }

    @Test
    void aNewNbuRateMovesASpreadRateButNeverAManualOne() throws Exception {
        nbuAnswers(today.minusDays(1), "40.0000", "47.0000");
        exchangeRateService.syncOfficialRates();
        String adminToken = tokenOf(createAdmin("admin-" + UUID.randomUUID() + "@bank.test"));

        setUsdRate(adminToken, """
                {"mode": "MANUAL", "buy": 39.80, "sell": 40.40}
                """).andExpect(status().isOk());
        setRate(adminToken, "EUR", """
                {"mode": "SPREAD", "spreadPercent": 2.00}
                """).andExpect(status().isOk());

        nbuAnswers(today, "41.0000", "48.0000");
        exchangeRateService.syncOfficialRates();

        mockMvc.perform(MockMvcRequestBuilders.get("/exchange-rates"))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].buy").value(39.8))
                .andExpect(jsonPath("$[0].sell").value(40.4))
                .andExpect(jsonPath("$[1].currency").value("EUR"))
                .andExpect(jsonPath("$[1].buy").value(47.04))
                .andExpect(jsonPath("$[1].sell").value(48.96));
    }

    @Test
    void ratesThatWouldLoseMoneyOrLookLikeATypoAreRejected() throws Exception {
        nbuAnswers(today, "41.5000", "48.0000");
        exchangeRateService.syncOfficialRates();
        String adminToken = tokenOf(createAdmin("admin-" + UUID.randomUUID() + "@bank.test"));

        // Buying at or above the selling price lets anyone earn by exchanging back and forth.
        setUsdRate(adminToken, """
                {"mode": "MANUAL", "buy": 42.00, "sell": 41.90}
                """).andExpect(status().isUnprocessableContent());
        // 4.15 instead of 41.5: a missed digit, far from the NBU rate.
        setUsdRate(adminToken, """
                {"mode": "MANUAL", "buy": 4.15, "sell": 41.90}
                """).andExpect(status().isUnprocessableContent());
        setUsdRate(adminToken, """
                {"mode": "SPREAD", "spreadPercent": 1.00, "buy": 41.00}
                """).andExpect(status().isUnprocessableContent());
        setRate(adminToken, "UAH", """
                {"mode": "SPREAD", "spreadPercent": 1.00}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void startupBootstrapNeverPromotesAnAccountAClientRegistered() {
        UUID clientId = createUser();
        String clientEmail = userService.getById(clientId).email();

        userService.createAdminIfAbsent(clientEmail, "an-attacker-would-like-this");

        assertThat(userService.getById(clientId).role()).isEqualTo(Role.CLIENT);
    }

    private void nbuAnswers(LocalDate date, String usd, String eur) {
        String nbuDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        wireMock.stubFor(get(urlPathEqualTo(NBU_PATH)).willReturn(okJson("""
                [
                  {"r030": 840, "txt": "US Dollar", "rate": %s, "cc": "USD", "exchangedate": "%s"},
                  {"r030": 978, "txt": "Euro", "rate": %s, "cc": "EUR", "exchangedate": "%s"},
                  {"r030": 826, "txt": "Pound Sterling", "rate": 55.1000, "cc": "GBP", "exchangedate": "%s"}
                ]
                """.formatted(usd, nbuDate, eur, nbuDate, nbuDate))));
    }

    private UUID createAdmin(String email) {
        userService.createAdminIfAbsent(email, "admin-password-123");
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private String tokenOf(UUID userId) {
        return accessTokenService.issue(userRepository.findById(userId).orElseThrow());
    }

    private ResultActions setUsdRate(String token, String body) throws Exception {
        return setRate(token, "USD", body);
    }

    private ResultActions setRate(String token, String currency, String body) throws Exception {
        var request = put("/admin/exchange-rates/{currency}", currency)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }
}
