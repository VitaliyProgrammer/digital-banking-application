package io.github.vitaliyprogrammer.banking.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.vitaliyprogrammer.banking.IntegrationTest;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Proof tests for "forgot password", end to end: the HTTP API, the database, and a real mail server. The client
 * gets a link by email and sets a new password with it; the link works once and expires; every old session ends;
 * the answer never reveals whether an address has an account; nobody can flood an inbox.
 */
class PasswordResetIntegrationTests extends IntegrationTest {

    private static final String OLD_PASSWORD = "old-password-123";

    private static final String NEW_PASSWORD = "brand-new-password-456";

    private static final String LINK_SUBJECT = "Відновлення пароля";

    private static final Pattern LINK_TOKEN = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aClientWhoForgotTheirPasswordSetsANewOneThroughTheEmailedLink() throws Exception {
        String email = register();
        String oldRefreshToken = JsonPath.read(login(email, OLD_PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        String token = requestResetAndReadLink(email);
        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());

        login(email, OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(email, NEW_PASSWORD).andExpect(status().isOk());
        // The session opened with the old password is over: whoever had it is out.
        mockMvc.perform(post("/authentication/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
        // And the owner is told, in case it was not them.
        waitUntil(() -> mailServer.countTo(email, "Пароль змінено"), 1);
        assertThat(mailServer.latestTextTo(email, "Пароль змінено")).contains("Пароль до вашого акаунта щойно змінено");
    }

    @Test
    void aLinkWorksOnceAndOnlyUntilItExpiresAndANewRequestReplacesTheOldLink() throws Exception {
        String email = register();

        String used = requestResetAndReadLink(email);
        confirm(used, NEW_PASSWORD).andExpect(status().isNoContent());
        confirm(used, "yet-another-password-789").andExpect(status().isBadRequest());

        String replaced = requestResetAndReadLink(email);
        String latest = requestResetAndReadLink(email);
        confirm(replaced, "yet-another-password-789").andExpect(status().isBadRequest());

        jdbcTemplate.update("UPDATE password_reset_tokens SET expires_at = now() - interval '1 minute' "
                + "WHERE used_at IS NULL AND user_id = (SELECT id FROM users WHERE email = ?)", email);
        confirm(latest, "yet-another-password-789").andExpect(status().isBadRequest());
    }

    @Test
    void anAddressWithoutAnAccountGetsTheSameAnswerAndNoEmail() throws Exception {
        String nobody = "nobody-" + UUID.randomUUID() + "@bank.local";

        requestReset(nobody).andExpect(status().isAccepted());

        Thread.sleep(1500);
        assertThat(mailServer.countTo(nobody)).isZero();
    }

    @Test
    void moreThanThreeRequestsAnHourAreSilentlyIgnored() throws Exception {
        String email = register();

        for (int i = 0; i < 5; i++) {
            requestReset(email).andExpect(status().isAccepted());
        }

        waitUntil(() -> mailServer.countTo(email), 3);
        Thread.sleep(1500);
        assertThat(mailServer.countTo(email)).isEqualTo(3);
    }

    private String register() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@bank.local";
        mockMvc.perform(post("/authentication/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + OLD_PASSWORD
                                + "\",\"firstName\":\"Olena\",\"lastName\":\"Demo\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/authentication/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/authentication/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions confirm(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/authentication/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + newPassword + "\"}"));
    }

    /** Asks for a link and reads it from the newest email, the way the client would. */
    private String requestResetAndReadLink(String email) throws Exception {
        // Count only the link emails: a "password changed" email from an earlier step may still be on its way.
        int before = mailServer.countTo(email, LINK_SUBJECT);
        requestReset(email).andExpect(status().isAccepted());
        waitUntil(() -> mailServer.countTo(email, LINK_SUBJECT), before + 1);

        Matcher link = LINK_TOKEN.matcher(mailServer.latestTextTo(email, LINK_SUBJECT));
        assertThat(link.find()).as("a reset link in the email").isTrue();
        return link.group(1);
    }

    /** The email leaves on its own thread after commit: give it a few seconds. */
    private static void waitUntil(IntSupplier count, int expected) throws InterruptedException {
        for (int attempt = 0; attempt < 50 && count.getAsInt() < expected; attempt++) {
            Thread.sleep(200);
        }
        assertThat(count.getAsInt()).as("emails received").isGreaterThanOrEqualTo(expected);
    }
}
