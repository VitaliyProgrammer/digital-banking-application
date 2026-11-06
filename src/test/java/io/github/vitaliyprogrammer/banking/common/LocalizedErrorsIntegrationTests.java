package io.github.vitaliyprogrammer.banking.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.vitaliyprogrammer.banking.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Errors over real HTTP, in the language the client asks for: the text changes, the status and the code do not.
 * Covers the three sources of an error text: a business error, a validation rule, and Spring MVC itself.
 */
class LocalizedErrorsIntegrationTests extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    private String accountId;

    @BeforeEach
    void clientWithAnAccount() throws Exception {
        String email = "lang-" + UUID.randomUUID() + "@bank.local";
        mockMvc.perform(post("/authentication/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "long-enough-password", "firstName": "Olena", "lastName": "Demo"}
                        """.formatted(email)))
                .andExpect(status().isCreated());
        token = JsonPath.read(mockMvc.perform(post("/authentication/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "long-enough-password"}
                                """.formatted(email)))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        accountId = JsonPath.read(openUahAccount(null).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    @ParameterizedTest(name = "Accept-Language: {0}")
    @CsvSource(delimiter = '|', nullValues = "none", value = {
            "uk                   | У вас уже є рахунок у UAH",
            "uk-UA,uk;q=0.9,en;q=0.8 | У вас уже є рахунок у UAH",
            "en                   | You already have an account in UAH",
            "de                   | You already have an account in UAH",
            "none                 | You already have an account in UAH"})
    void businessErrorSpeaksTheClientsLanguage(String language, String detail) throws Exception {
        openUahAccount(language)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(detail))
                // The same code in every language: a program reacts to it, never to the text.
                .andExpect(jsonPath("$.code").value("account-already-exists"));
    }

    @Test
    void valuesInsideTheTextAreKept() throws Exception {
        String cardId = issueCard("UNIVERSAL");
        mockMvc.perform(authorized(put("/cards/" + cardId + "/pin"), "uk").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPin": "4826"}
                                """))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(authorized(post("/cards/" + cardId + "/pin/verification"), "uk")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"pin": "0007"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Неправильний PIN. Залишилось спроб: 2"))
                .andExpect(jsonPath("$.code").value("wrong-pin"));
    }

    @Test
    void validationSpeaksTheClientsLanguageToo() throws Exception {
        String cardId = issueCard("VIRTUAL");

        mockMvc.perform(authorized(put("/cards/" + cardId + "/pin"), "uk").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPin": "12"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Запит містить помилки"))
                .andExpect(jsonPath("$.code").value("validation-failed"))
                // Our own rule, from messages_uk.properties.
                .andExpect(jsonPath("$.errors.newPin").value("має складатися з 4 цифр"));

        mockMvc.perform(authorized(put("/cards/" + cardId + "/pin"), "en").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPin": "12"}
                                """))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.newPin").value("must be 4 digits"));
    }

    @Test
    void springsOwnErrorsAreTranslated() throws Exception {
        // No Idempotency-Key: Spring MVC refuses the request before any of our code runs.
        mockMvc.perform(authorized(post("/payments/top-up"), "uk").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "%s", "amount": 100.00}
                                """.formatted(accountId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Бракує обовʼязкового заголовка «Idempotency-Key»"));
    }

    private ResultActions openUahAccount(String language) throws Exception {
        return mockMvc.perform(authorized(post("/accounts"), language).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currency": "UAH"}
                        """));
    }

    private String issueCard(String type) throws Exception {
        return JsonPath.read(mockMvc.perform(authorized(post("/cards"), null).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "%s", "type": "%s"}
                                """.formatted(accountId, type)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String language) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (language != null) {
            request.header(HttpHeaders.ACCEPT_LANGUAGE, language);
        }
        return request;
    }
}
