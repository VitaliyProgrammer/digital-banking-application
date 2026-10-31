package io.github.vitaliyprogrammer.banking.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.vitaliyprogrammer.banking.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Proof tests for phase 0.2: the full auth flow works end to end on a real database,
 * and a stolen refresh token cannot be replayed.
 */
class AuthenticationFlowIntegrationTests extends IntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registeredClientLogsInAndReadsOwnProfile() throws Exception {
        String email = register();
        String accessToken = JsonPath.read(login(email, PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.accessToken");

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownEmailAndWrongPasswordGetIdenticalResponse() throws Exception {
        String email = register();

        String wrongPassword = login(email, "wrong-password").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("nobody-" + email, PASSWORD).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
    }

    @Test
    void reusedRefreshTokenRevokesAllSessions() throws Exception {
        String email = register();
        String firstRefreshToken = JsonPath.read(
                login(email, PASSWORD).andReturn().getResponse().getContentAsString(), "$.refreshToken");

        String rotatedRefreshToken = JsonPath.read(refresh(firstRefreshToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        // An attacker replays the old token: rejected, and as a reaction every session is revoked...
        refresh(firstRefreshToken).andExpect(status().isUnauthorized());
        // ...so even the legitimate, freshly rotated token no longer works.
        refresh(rotatedRefreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void sixthLoginAttemptIsRejectedEvenWithTheRightPassword() throws Exception {
        String email = register();

        for (int attempt = 1; attempt <= 5; attempt++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }

        // Someone guessing passwords is stopped for 15 minutes, even if the next guess happens to be right.
        login(email, PASSWORD).andExpect(status().isTooManyRequests());
    }

    private String register() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/authentication/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "firstName": "Taras", "lastName": "Shevchenko"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return email;
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/authentication/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken": "%s"}
                        """.formatted(refreshToken)));
    }
}
